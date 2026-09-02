package in.gov.ipie.service.template.scanning;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.gov.ipie.common.filestorage.scanning.VirusScanner;

/**
 * Chooses the {@link VirusScanner} implementation: {@link ClamAvVirusScanner} when {@code
 * ipie.scanning.clamav.host} is configured, {@link FailClosedVirusScanner} otherwise (never
 * "assume clean" - master standards doc, file-upload rules, section 3).
 *
 * <p><b>Swapping to a cloud-native scanner later</b> (e.g. AWS malware protection, once that has
 * organizational approval): add a new class implementing {@link VirusScanner} (mirroring how
 * {@link ClamAvVirusScanner} does it), add a {@code @Bean} method here gated by whatever
 * config property signals it's configured (mirroring {@code ipie.scanning.clamav.host}'s role),
 * and give it precedence over {@link ClamAvVirusScanner} the same way {@code
 * EventPublisherConfig} gives Kafka precedence over RabbitMQ - {@code
 * @ConditionalOnMissingBean(VirusScanner.class)} on the lower-priority bean. No caller of {@link
 * VirusScanner} changes.
 */
@Configuration
public class VirusScanConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ipie.scanning.clamav", name = "host")
    public VirusScanner clamAvVirusScanner(
            @Value("${ipie.scanning.clamav.host}") String host,
            @Value("${ipie.scanning.clamav.port:3310}") int port,
            @Value("${ipie.scanning.clamav.timeout-ms:10000}") int timeoutMillis) {
        return new ClamAvVirusScanner(host, port, timeoutMillis);
    }

    @Bean
    @ConditionalOnMissingBean(VirusScanner.class)
    public VirusScanner failClosedVirusScanner() {
        return new FailClosedVirusScanner();
    }
}
