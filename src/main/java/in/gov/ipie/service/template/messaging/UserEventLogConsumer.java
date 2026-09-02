package in.gov.ipie.service.template.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;

/**
 * Reference consumer for this service's own {@code ipie-service-template.events} topic -
 * demonstrates the required consumer-side idempotency pattern (master standards doc, section 9:
 * "Consumers must handle duplicate delivery") via {@link IdempotentEventHandler}, the same way
 * {@code KafkaEventPublisher} demonstrates the producer side. {@code @ConditionalOnProperty} at
 * the class level - not just on {@code EventConsumerConfig}'s factory bean - is essential: Spring
 * Boot's own Kafka auto-configuration provides a default listener container factory whenever
 * {@code spring-kafka} is on the classpath, regardless of whether {@code EventConsumerConfig}'s
 * conditional factory fires, so an unconditional {@code @Component} here would still bind and
 * attempt a connection (to Spring Boot's own default, {@code localhost:9092}) even when Kafka
 * isn't the configured broker.
 */
@Component
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
class UserEventLogConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(UserEventLogConsumer.class);

    private final ProcessedEventStore processedEventStore;

    UserEventLogConsumer(ProcessedEventStore processedEventStore) {
        this.processedEventStore = processedEventStore;
    }

    @KafkaListener(topics = "${ipie.events.kafka.topic}", groupId = "${spring.application.name}.user-event-log")
    void onEvent(EventEnvelope<?> event) {
        IdempotentEventHandler.handle(event.eventId(), processedEventStore,
                () -> LOG.info("Processed event [{} v{}] {} for entity {}",
                        event.eventType(), event.eventVersion(), event.eventId(), event.data()));
    }
}
