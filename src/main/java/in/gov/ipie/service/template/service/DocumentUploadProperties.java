package in.gov.ipie.service.template.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Groups {@code DocumentService}'s two configuration values (as opposed to its six collaborator
 * beans) into one constructor parameter, the same reasoning as {@code AuditMetadata} bundling the
 * audit columns - keeps {@code DocumentService}'s constructor at 7 parameters instead of 8
 * (master standards doc binding rules, Section 13.1).
 */
@Component
public class DocumentUploadProperties {

    private final String serviceName;
    private final long maxFileSizeBytes;

    public DocumentUploadProperties(
            @Value("${spring.application.name}") String serviceName,
            @Value("${ipie.documents.max-file-size-bytes:26214400}") long maxFileSizeBytes) {
        this.serviceName = serviceName;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public String serviceName() {
        return serviceName;
    }

    public long maxFileSizeBytes() {
        return maxFileSizeBytes;
    }
}
