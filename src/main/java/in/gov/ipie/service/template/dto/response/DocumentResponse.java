package in.gov.ipie.service.template.dto.response;

import java.time.Instant;

import in.gov.ipie.service.template.domain.DocumentStatus;

public record DocumentResponse(
        String id,
        String caseId,
        String docType,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String sha256Hash,
        DocumentStatus status,
        int versionNumber,
        String supersedesId,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
