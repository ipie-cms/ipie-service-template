package in.gov.ipie.service.template.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;

class UserEventLogConsumerTest {

    private final ProcessedEventStore processedEventStore = mock(ProcessedEventStore.class);
    private final UserEventLogConsumer consumer = new UserEventLogConsumer(processedEventStore);

    @Test
    void onEvent_marksANewEventAsProcessed() {
        EventEnvelope<String> event = EventEnvelope.create("USER_CREATED", 1, "test", null, null, "user-1");
        when(processedEventStore.isProcessed(event.eventId())).thenReturn(false);

        consumer.onEvent(event);

        verify(processedEventStore).isProcessed(event.eventId());
        verify(processedEventStore).markProcessed(event.eventId());
    }

    @Test
    void onEvent_skipsAnAlreadyProcessedEvent() {
        EventEnvelope<String> event = EventEnvelope.create("USER_CREATED", 1, "test", null, null, "user-1");
        when(processedEventStore.isProcessed(event.eventId())).thenReturn(true);

        consumer.onEvent(event);

        verify(processedEventStore).isProcessed(event.eventId());
        verifyNoMoreInteractions(processedEventStore);
    }
}
