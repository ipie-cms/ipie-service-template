package in.gov.ipie.service.template.examples;

import java.util.List;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.utils.collection.CollectionUtils;
import in.gov.ipie.common.utils.datetime.DateTimeUtils;
import in.gov.ipie.common.utils.exception.ExceptionUtils;
import in.gov.ipie.common.utils.id.IdGenerator;
import in.gov.ipie.common.utils.json.JsonUtils;
import in.gov.ipie.common.utils.masking.DataMasking;
import in.gov.ipie.common.utils.network.NetworkUtils;
import in.gov.ipie.common.utils.text.Strings;
import in.gov.ipie.common.utils.validation.ValidationUtils;

/**
 * Example-only reference code for {@code common-utils} - not wired into any real request flow, a
 * new service's actual business code should call these same static methods directly at whichever
 * call site actually needs them (e.g. {@code DataMasking.maskEmail} right before a log statement
 * that would otherwise print a raw email address).
 *
 * <p>See {@code SERVICE_CLASS_REFERENCE.md} for where each already-used common-lib (common-security,
 * common-audit, common-events, common-observability, common-web, common-file-storage) is
 * demonstrated in this service's real User/Document flow instead of a standalone example.
 */
@Component
public class CommonUtilsExample {

    /** {@code DataMasking.maskEmail} - call this before logging/displaying an email, never log the raw value. */
    public String maskEmailForLogging(String email) {
        return DataMasking.maskEmail(email);
    }

    /** {@code DataMasking.maskAadhaar} - only the last four digits are ever shown. */
    public String maskAadhaarForLogging(String aadhaar) {
        return DataMasking.maskAadhaar(aadhaar);
    }

    /** {@code IdGenerator.newReferenceNumber} - a short, human-readable business reference, e.g. {@code "DOC-20260714-4F2A"}. */
    public String generateHumanReadableReference(String prefix) {
        return IdGenerator.newReferenceNumber(prefix);
    }

    /** {@code IdGenerator.newId} - an opaque id (a UUID string) for cases that don't need a human-readable reference. */
    public String newOpaqueId() {
        return IdGenerator.newId();
    }

    /** {@code DateTimeUtils.nowUtc}/{@code toIso8601} - the platform's ISO 8601 date convention (master standards doc, 5.3). */
    public String currentTimestampIso8601() {
        return DateTimeUtils.toIso8601(DateTimeUtils.nowUtc());
    }

    /** {@code Strings.isBlank}/{@code defaultIfBlank} - null/blank-safe helpers instead of ad hoc {@code == null} checks. */
    public String normalizeOptionalField(String rawValue, String fallback) {
        return Strings.isBlank(rawValue) ? fallback : rawValue;
    }

    /** {@code JsonUtils.toJson} - serializing a payload outside the Spring MVC request/response cycle, e.g. an event body. */
    public String serializeForEventPayload(Object value) {
        return JsonUtils.toJson(value);
    }

    /** {@code ValidationUtils.isValidPan} - a format check feeding into a {@code ValidationFailedException}, not a replacement for it. */
    public boolean isWellFormedPan(String pan) {
        return ValidationUtils.isValidPan(pan);
    }

    /** {@code CollectionUtils.partition} - splitting a batch of ids into fixed-size chunks, e.g. for a downstream API's page-size limit. */
    public List<List<String>> chunkForDownstreamCall(List<String> ids, int chunkSize) {
        return CollectionUtils.partition(ids, chunkSize);
    }

    /** {@code NetworkUtils.maskIpv4} - masking a client IP before it's written to a log line. */
    public String maskClientIpForLogging(String ipv4) {
        return NetworkUtils.maskIpv4(ipv4);
    }

    /** {@code ExceptionUtils.getRootCause} - finding the innermost cause of a caught exception before logging/reporting it. */
    public String describeRootCause(Throwable throwable) {
        return ExceptionUtils.getRootCause(throwable).getMessage();
    }
}
