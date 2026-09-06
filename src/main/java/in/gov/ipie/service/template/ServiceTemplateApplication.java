package in.gov.ipie.service.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Approved starting point for every iPIE backend microservice. When creating a new service:
 * rename this class, its package (in.gov.ipie.service.template -&gt; in.gov.ipie.&lt;service&gt;), the
 * Gradle module and {@code spring.application.name} as the very first commit - everything else
 * (layering, cross-cutting wiring, Docker, CI) stays as-is.
 *
 * <p>{@code @EnableScheduling} drives {@code OutboxRelayScheduler} - the transactional outbox
 * relay (master standards doc, section 9) - not any business-specific scheduled job.
 */
@SpringBootApplication
@EnableScheduling
public class ServiceTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceTemplateApplication.class, args);
    }
}
