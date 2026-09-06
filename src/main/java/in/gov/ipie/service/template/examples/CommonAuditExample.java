package in.gov.ipie.service.template.examples;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.audit.AuditRecorder;
import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEvent;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.observability.correlation.LoggingContext;
import in.gov.ipie.common.security.context.CurrentUserProvider;

/**
 * Example-only reference code for {@code common-audit} - not wired into any real request flow.
 * This template's own {@code UserServiceImpl}/{@code DocumentServiceImpl} already use both paths
 * for real; this class puts the two side by side so the choice between them is visible in one
 * place.
 *
 * <p><b>{@code @Auditable} is the default and the manual path is the exception.</b> The annotation
 * records exactly one event per <em>successful</em> invocation, which is precisely why a rejection
 * that throws - a failed permission check, a malware detection - can never be captured by it: the
 * method did not return, so no event is written. Those are the cases that call
 * {@link AuditRecorder#record} directly, and they are the only ones that should.
 *
 * <p>{@code @Auditable} is AOP advice, so it only fires on a call that arrives through the Spring
 * proxy. A method calling another {@code @Auditable} method on {@code this} silently records
 * nothing - the same self-invocation rule that applies to {@code @Transactional}.
 */
@Component
public class CommonAuditExample {

    private final AuditRecorder auditRecorder;
    private final CurrentUserProvider currentUserProvider;
    private final String serviceName;

    public CommonAuditExample(
            AuditRecorder auditRecorder,
            CurrentUserProvider currentUserProvider,
            @Value("${spring.application.name}") String serviceName) {
        this.auditRecorder = auditRecorder;
        this.currentUserProvider = currentUserProvider;
        this.serviceName = serviceName;
    }

    /**
     * The annotation-driven path - the one a service should reach for. Every attribute except
     * {@code eventType}/{@code action}/{@code entityType} is a SpEL expression over this method's
     * own parameters, with {@code #result} additionally in scope once it has returned.
     *
     * <p>{@code comment} reads a value the caller supplied rather than a literal - it is the
     * human's stated reason for the action, not a description of the action ({@code action}
     * already carries that). {@code oldValue} is deliberately absent here: it is evaluated
     * <em>before</em> the method runs, so it can only see the arguments, and a caller that needs
     * the prior state recorded has to fetch it and pass it in.
     *
     * <p>{@code actorUserId}, {@code sourceIp}, {@code serviceName}, {@code correlationId} and
     * {@code occurredAt} are filled in by {@code AuditAspect} - never passed by business code.
     */
    @Auditable(
            eventType = AuditEventType.BUSINESS,
            action = "EXAMPLE_RECORD_APPROVED",
            entityType = "EXAMPLE",
            entityId = "#recordId",
            comment = "#approverReason",
            newValue = "#result")
    public String approveRecord(UUID recordId, String approverReason) {
        return "APPROVED";
    }

    /**
     * The manual path, for the case {@code @Auditable} structurally cannot cover: the action was
     * <em>refused</em>, so the annotated method would throw and record nothing, yet the refusal is
     * the very thing that has to leave a trail.
     *
     * <p>{@link AuditEventType#SECURITY} rather than {@code BUSINESS} - a rejected action is an
     * access/security event, and the two categories are queried separately (master standards doc,
     * 5.7). Note this is also where the aspect's implicit fields have to be supplied by hand:
     * the actor from {@link CurrentUserProvider} (falling back to {@code "system"} for a scheduled
     * or event-driven caller, which has no authenticated user at all) and the correlation id from
     * {@link LoggingContext}, which is what ties the record to the request's log lines.
     *
     * <p>{@code sourceIp} is left null here on purpose: this example has no request to read it
     * from. A real refusal raised inside a controller-served request should pass
     * {@code HttpRequestUtils.clientIp(request)} - see {@code CommonWebExample}.
     */
    public void recordRefusal(UUID recordId, String refusalDetail) {
        auditRecorder.record(new AuditEvent(
                AuditEventType.SECURITY,
                "EXAMPLE_RECORD_REFUSED",
                "EXAMPLE",
                recordId.toString(),
                null,
                currentUserProvider.current().map(user -> user.userId()).orElse("system"),
                null,
                serviceName,
                null,
                null,
                refusalDetail,
                LoggingContext.correlationId(),
                Instant.now()));
    }
}
