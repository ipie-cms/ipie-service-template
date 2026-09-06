package in.gov.ipie.service.template.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import in.gov.ipie.service.template.dto.response.DocumentResponse;
import in.gov.ipie.service.template.domain.Document;

/**
 * Domain -&gt; API response mapping for Document. Hand-written rather than MapStruct: there is no
 * request-DTO side to this mapping (uploads arrive as multipart form fields, not a JSON body), so
 * MapStruct's main value - keeping two DTO shapes in sync - doesn't apply here.
 *
 * <p>Deliberately never exposes {@code storageKey} - callers get a short-lived presigned URL from
 * {@code DocumentController#downloadUrl} instead of a raw storage pointer (master standards doc,
 * file-upload rules, section 5).
 */
@Component
public class DocumentApiMapper {

    public DocumentResponse toResponse(Document document) {
        UUID supersedesId = document.getVersion().supersedesId();
        return new DocumentResponse(
                document.getId().toString(),
                document.getContext().caseId(),
                document.getContext().docType(),
                document.getFile().originalFilename(),
                document.getFile().contentType(),
                document.getFile().sizeBytes(),
                document.getFile().sha256Hash(),
                document.getStatus(),
                document.getVersion().number(),
                supersedesId != null ? supersedesId.toString() : null,
                document.getAuditMetadata().createdAt(),
                document.getAuditMetadata().updatedAt(),
                document.getAuditMetadata().version());
    }
}
