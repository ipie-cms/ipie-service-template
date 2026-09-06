package in.gov.ipie.service.template.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;

/**
 * RabbitMQ counterpart to {@code UserEventLogConsumer} - demonstrates the same required
 * consumer-side idempotency pattern (master standards doc, section 9) when this service is
 * configured to use RabbitMQ instead of Kafka (standby in case Kafka doesn't get organizational
 * clearance). {@code @ConditionalOnProperty} at the class level - not just on the listener method
 * - is essential here: Spring Boot's own RabbitMQ auto-configuration provides a default listener
 * container factory whenever {@code spring-boot-starter-amqp} is on the classpath, regardless of
 * whether {@code RabbitConsumerConfig}'s conditional factory fires, so an unconditional
 * {@code @Component} would still bind and attempt a connection even when RabbitMQ isn't the
 * configured broker.
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
class RabbitUserEventLogConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitUserEventLogConsumer.class);

    private final ProcessedEventStore processedEventStore;

    RabbitUserEventLogConsumer(ProcessedEventStore processedEventStore) {
        this.processedEventStore = processedEventStore;
    }

    @RabbitListener(queues = "${ipie.events.rabbitmq.queue}")
    void onEvent(EventEnvelope<?> event) {
        IdempotentEventHandler.handle(event.eventId(), processedEventStore,
                () -> LOG.info("Processed event [{} v{}] {} for entity {}",
                        event.eventType(), event.eventVersion(), event.eventId(), event.data()));
    }
}
