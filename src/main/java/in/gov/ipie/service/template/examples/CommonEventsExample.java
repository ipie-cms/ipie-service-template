package in.gov.ipie.service.template.examples;

import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.common.events.deadletter.DeadLetterSupport;
import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.common.events.outbox.OutboxStore;
import in.gov.ipie.common.observability.correlation.LoggingContext;

/**
 * Example-only reference code for {@code common-events} - not wired into any real request flow.
 * The template's {@code UserServiceImpl}/{@code messaging} package already run this pipeline for
 * real; this class isolates the three things a service developer actually writes, because the rest
 * of the pipeline (the JPA stores, {@code OutboxRelay}, {@code OutboxRelayScheduler}, the broker
 * bindings) is the platform's and is configured, not coded.
 *
 * <p><b>Never inject {@code EventPublisher}.</b> Publishing straight to the broker from business
 * code is a dual write: the row commits and the publish fails, or the reverse, and nothing
 * reconciles them. Writing to {@link OutboxStore} inside the <em>same</em> transaction as the
 * business change is what makes the two atomic - and the platform's own ArchUnit rule
 * {@code applicationDoesNotCallEventPublisherDirectly} fails the build for a service that forgets
 * this.
 *
 * <p>The relay guarantees at-least-once delivery, not exactly-once. That is not a defect to work
 * around on the publish side - it is why every consumer must go through
 * {@link IdempotentEventHandler}. The two together are what make processing effectively
 * exactly-once.
 */
@Component
public class CommonEventsExample {

    private final OutboxStore outboxStore;
    private final ProcessedEventStore processedEventStore;
    private final String serviceName;

    public CommonEventsExample(
            OutboxStore outboxStore,
            ProcessedEventStore processedEventStore,
            @Value("${spring.application.name}") String serviceName) {
        this.outboxStore = outboxStore;
        this.processedEventStore = processedEventStore;
        this.serviceName = serviceName;
    }

    /**
     * The publish side. {@code @Transactional} is load-bearing rather than decorative: the outbox
     * row and whatever business write precedes it must share one transaction, or the pattern buys
     * nothing.
     *
     * <p>{@code eventVersion} is the <em>contract</em> version, not a sequence number. Bump it only
     * for a change that would break an existing consumer, and keep publishing the old version
     * alongside the new one until every consumer has migrated.
     *
     * <p>The correlation id comes from {@link LoggingContext}, not from a parameter - that is what
     * lets a log search follow one request across the publishing service, the broker and every
     * consumer. Passing {@code null} here is the single easiest way to make a cross-service flow
     * untraceable.
     */
    @Transactional
    public void publishRecordApproved(UUID recordId) {
        outboxStore.save(EventEnvelope.create(
                "EXAMPLE_RECORD_APPROVED",
                1,
                serviceName,
                LoggingContext.correlationId(),
                null,
                Map.of("recordId", recordId.toString())));
    }

    /**
     * The consume side. {@code check-then-mark} around the handler, so a redelivery of an event
     * already applied does nothing instead of applying it twice.
     *
     * <p>The mark is written only after {@code applyToOwnData} returns, and both belong in one
     * transaction with the handler's own writes - a handler that succeeds but crashes before the
     * mark is redelivered, which is correct; a handler that marks first and then fails has lost the
     * event permanently.
     */
    @Transactional
    public void handleInbound(EventEnvelope<?> event, Runnable applyToOwnData) {
        IdempotentEventHandler.handle(event.eventId(), processedEventStore, applyToOwnData);
    }

    /**
     * What a RabbitMQ consumer's {@code @Bean Queue} method in a service's own {@code messaging}
     * package returns - never {@code new Queue(name)}.
     *
     * <p>A plain queue has no dead-letter exchange, and a message the consumer can never handle then
     * has two possible fates, both bad: Spring AMQP requeues it by default and the consumer spins on
     * it forever, or requeue is turned off and it is dropped with no trace. This routes it to the
     * one shared {@code ipie.events.dlq} instead, where RabbitMQ's own {@code x-death} header
     * already records which queue it came from, how often, and why.
     */
    public Queue exampleConsumerQueue() {
        return DeadLetterSupport.workQueue("example.consumer.queue");
    }
}
