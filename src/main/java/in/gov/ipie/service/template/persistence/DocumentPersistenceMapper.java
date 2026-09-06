package in.gov.ipie.service.template.persistence;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.service.template.domain.Document;
import in.gov.ipie.service.template.domain.DocumentContext;
import in.gov.ipie.service.template.domain.DocumentFile;
import in.gov.ipie.service.template.domain.DocumentVersion;

/**
 * Converts between the JPA entity and the domain model - hand-written (not MapStruct) for the
 * same reason as {@code UserPersistenceMapper}: it assembles {@link AuditMetadata} (and here,
 * also {@link DocumentContext}/{@link DocumentFile}/{@link DocumentVersion}) from individual flat
 * columns. Public so the {@code repositoryimpl} subpackage can use it across the package boundary.
 */
@Component
public class DocumentPersistenceMapper {

    public Document toDomain(DocumentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        AuditMetadata auditMetadata = new AuditMetadata(
                entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getVersion(),
                entity.isActive(), entity.getDeletedAt(), entity.getDeletedBy());
        DocumentContext context = new DocumentContext(entity.getCaseId(), entity.getDocType());
        DocumentFile file = new DocumentFile(
                entity.getOriginalFilename(), entity.getStorageKey(), entity.getContentType(),
                entity.getSizeBytes(), entity.getSha256Hash());
        DocumentVersion version = new DocumentVersion(entity.getVersionNumber(), entity.getSupersedesId());
        return new Document(entity.getId(), context, file, entity.getStatus(), version, entity.getRetentionUntil(), auditMetadata);
    }

    public DocumentJpaEntity toNewEntity(Document document) {
        return new DocumentJpaEntity(
                document.getId(), document.getContext(), document.getFile(), document.getStatus(),
                document.getVersion(), document.getRetentionUntil());
    }
}
