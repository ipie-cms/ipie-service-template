package in.gov.ipie.service.template.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.resilience.exception.TransientDependencyException;

/**
 * Plain unit test of {@link CommonResilienceExample}'s own method body, run without a Spring
 * context - so {@code @Retry}/{@code @CircuitBreaker} are inert here (resilience4j's AOP aspects
 * only apply to calls made through a Spring-managed proxy) and this only proves the simulated
 * failure/recovery logic itself is correct. That the annotations actually apply
 * common-resilience's shared defaults (not resilience4j's own factory defaults) is proven end to
 * end by common-resilience's own {@code ResilienceDefaultsBehaviorTest}.
 */
class CommonResilienceExampleTest {

    private final CommonResilienceExample example = new CommonResilienceExample();

    @Test
    void failsTransientlyTwiceThenSucceedsOnTheThirdCall() {
        AtomicInteger attempts = new AtomicInteger();

        assertThat(callUntilSuccess(attempts)).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(3);
    }

    private String callUntilSuccess(AtomicInteger attempts) {
        while (true) {
            try {
                return example.callFlakyDependency(attempts);
            } catch (TransientDependencyException e) {
                // Simulates what @Retry does automatically when this method is called through a
                // real Spring-managed proxy - retried here by hand since there is no proxy in a
                // plain unit test.
            }
        }
    }
}
