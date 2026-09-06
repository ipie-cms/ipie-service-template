package in.gov.ipie.service.template.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.gov.ipie.common.persistence.IntegrityViolations;

/**
 * Publishes this service's constraint declarations so the shared error boundary can read them.
 *
 * <p>The repository's {@code catch} only fires when Hibernate flushes inside the repository call.
 * With the insert deferred to commit, the violation surfaces after the repository has returned and
 * {@code GlobalExceptionHandler} is the only thing left on the stack - so the same declaration has
 * to be reachable as a bean. A service copied from this template should register its own tables the
 * same way; one missed here is a 409 that arrives as a 500.
 */
@Configuration
class IntegrityViolationsConfig {

    @Bean
    IntegrityViolations userIntegrityViolations() {
        return UserRepositoryImpl.VIOLATIONS;
    }
}
