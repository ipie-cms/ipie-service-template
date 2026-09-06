package in.gov.ipie.service.template.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CommonUtilsExampleTest {

    private final CommonUtilsExample example = new CommonUtilsExample();

    @Test
    void masksAnEmailInsteadOfLoggingItInTheClear() {
        assertThat(example.maskEmailForLogging("jdoe@example.com")).isEqualTo("jd***@example.com");
    }

    @Test
    void masksAllButTheLastFourAadhaarDigits() {
        assertThat(example.maskAadhaarForLogging("1234 5678 9012")).isEqualTo("XXXX XXXX 9012");
    }

    @Test
    void generatesAHumanReadableReferenceWithTheGivenPrefix() {
        String reference = example.generateHumanReadableReference("DOC");

        assertThat(reference).startsWith("DOC-").matches("DOC-\\d{8}-[A-Z0-9]{4}");
    }

    @Test
    void generatesAnOpaqueUuidBasedId() {
        assertThat(example.newOpaqueId()).matches("[0-9a-f-]{36}");
    }

    @Test
    void formatsTheCurrentTimestampAsIso8601() {
        assertThat(example.currentTimestampIso8601()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z");
    }

    @Test
    void normalizeOptionalField_returnsTheFallbackOnlyWhenTheRawValueIsBlank() {
        assertThat(example.normalizeOptionalField("  ", "default")).isEqualTo("default");
        assertThat(example.normalizeOptionalField("real-value", "default")).isEqualTo("real-value");
    }

    @Test
    void serializesAPayloadForAnEventBody() {
        assertThat(example.serializeForEventPayload(Map.of("k", "v"))).isEqualTo("{\"k\":\"v\"}");
    }

    @Test
    void validatesPanFormat() {
        assertThat(example.isWellFormedPan("ABCDE1234F")).isTrue();
        assertThat(example.isWellFormedPan("not-a-pan")).isFalse();
    }

    @Test
    void chunksIdsForADownstreamCall() {
        assertThat(example.chunkForDownstreamCall(List.of("a", "b", "c"), 2))
                .containsExactly(List.of("a", "b"), List.of("c"));
    }

    @Test
    void masksTheLastOctetOfAClientIp() {
        assertThat(example.maskClientIpForLogging("192.168.1.42")).isEqualTo("192.168.1.***");
    }

    @Test
    void describesTheRootCauseOfAWrappedException() {
        Throwable root = new IllegalStateException("connection reset");
        Throwable wrapped = new RuntimeException("call failed", root);

        assertThat(example.describeRootCause(wrapped)).isEqualTo("connection reset");
    }
}
