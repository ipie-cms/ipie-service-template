package in.gov.ipie.service.template.examples;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.resilience.exception.TransientDependencyException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Example-only reference code for {@code common-resilience} - not wired into any real outbound
 * call. Real business code adds these same two annotations directly to whichever method actually
 * makes the outbound/inter-service call (e.g. a method wrapping a downstream HTTP call, or - as
 * this template already does for a real outbound dependency - {@code ClamAvVirusScanner.scan}
 * could adopt the same pattern once it needs to tolerate a brief ClamAV restart rather than
 * failing the whole upload on the very first connection blip).
 *
 * <p>No per-service {@code resilience4j.*} YAML is needed here - the {@code name = "..."} on each
 * annotation is all that's required; both inherit {@code common-resilience}'s shared {@code
 * configs.default} (max 3 attempts, exponential backoff + jitter, opens the circuit after 5 of the
 * last 10 calls fail) automatically. Only {@link TransientDependencyException}, {@link
 * java.io.IOException} and {@link java.util.concurrent.TimeoutException} are ever retried - see
 * this class's test, and {@code common-resilience}'s own {@code ResilienceDefaultsBehaviorTest},
 * for proof the shared defaults (not resilience4j's own factory defaults) are what actually apply.
 */
@Component
public class CommonResilienceExample {

    /**
     * Simulates a dependency that fails transiently on its first two attempts, then succeeds -
     * wrapping the failure in {@link TransientDependencyException} is what opts a failure into the
     * shared default retry/circuit-breaker behavior; a functional/business error must never be
     * wrapped this way; see {@link TransientDependencyException}'s Javadoc.
     */
    @Retry(name = "example-dependency")
    @CircuitBreaker(name = "example-dependency")
    public String callFlakyDependency(AtomicInteger attemptsSoFar) {
        if (attemptsSoFar.incrementAndGet() < 3) {
            throw new TransientDependencyException("simulated transient failure on attempt " + attemptsSoFar.get());
        }
        return "ok";
    }
}
