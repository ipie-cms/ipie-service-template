package in.gov.ipie.service.template.examples;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.correlation.CorrelationConstants;
import in.gov.ipie.common.observability.correlation.LoggingContext;

/**
 * Example-only reference code for {@code common-observability} - not wired into any real request
 * flow.
 *
 * <p><b>The correlation id needs no code at all.</b> {@code CorrelationIdFilter} runs first in the
 * filter chain, reads {@code X-Correlation-Id} off the inbound request (minting one if it is absent
 * or does not look like an id), publishes it to the SLF4J MDC for the request's lifetime, echoes it
 * back on the response, and clears the MDC in a {@code finally}. Every log line a service writes
 * during that request already carries it. Business code only reaches for {@link LoggingContext}
 * to add the <em>business</em> fields the filter cannot know - the case and the user an operation
 * concerns (master standards doc, 5.6).
 *
 * <p><b>The one real trap is off-request work.</b> The MDC is a thread-local and the filter only
 * clears the request thread. A scheduled job, an {@code @Async} method or a broker consumer starts
 * with an empty context and, worse, keeps whatever it puts there for the next task the pooled
 * thread runs - so an unrelated later job logs someone else's case id. Off-request code must set
 * the context itself and clear it in a {@code finally}; see {@link #processOffRequest}.
 */
@Component
public class CommonObservabilityExample {

    private static final Logger log = LoggerFactory.getLogger(CommonObservabilityExample.class);

    /**
     * The in-request use: add the business fields, log normally. Nothing is cleared here - the
     * filter's {@code finally} owns that, and clearing mid-request would strip the correlation id
     * from every line after this one.
     */
    public void handleInRequest(String caseId, String userId) {
        LoggingContext.putCaseId(caseId);
        LoggingContext.putUserId(userId);
        log.info("Example operation accepted");
    }

    /**
     * The off-request use. The correlation id has to be seeded from whatever carried it here - the
     * envelope of the event being consumed, or the outbox row - because there is no inbound request
     * for the filter to have read it from, and a generated one would correlate with nothing.
     *
     * <p>{@link LoggingContext#clear()} in a {@code finally} is not tidiness. Without it the next
     * task scheduled onto this pooled thread inherits this task's case id and logs it as its own.
     */
    public void processOffRequest(String correlationIdFromEvent, String caseId, Runnable work) {
        try {
            LoggingContext.putCorrelationId(correlationIdFromEvent != null
                    ? correlationIdFromEvent
                    : UUID.randomUUID().toString());
            LoggingContext.putCaseId(caseId);
            work.run();
        } finally {
            LoggingContext.clear();
        }
    }

    /**
     * The id to stamp on anything that leaves this service and has to be tied back to these logs -
     * an outbox event's envelope, an audit record, an outbound call's header (whose name is
     * {@link CorrelationConstants#CORRELATION_ID_HEADER}; {@code InterServiceClient} already sets
     * it for you).
     */
    public String currentCorrelationId() {
        return LoggingContext.correlationId();
    }

    /**
     * The id shown to a <em>user</em> so a support report can be tied back to the logs - the
     * distributed trace id when tracing is active, falling back to the correlation id, which always
     * exists. {@code GlobalExceptionHandler} puts this on every {@code ApiError} automatically;
     * call it directly only when building a reference outside the error path.
     */
    public String userFacingTraceId() {
        return LoggingContext.traceId();
    }
}
