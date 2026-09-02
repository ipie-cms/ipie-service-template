package in.gov.ipie.service.template.domain;

import java.time.Instant;
import java.util.UUID;

import in.gov.ipie.common.core.model.AuditMetadata;

/**
 * The Document domain model - metadata only, never the file bytes (those live in {@code
 * FileStorage}, addressed by {@code getFile().storageKey()}). Independent of the JPA entity, same
 * reasoning as {@link User} (master standards doc, section 16). Fields are grouped into value
 * objects ({@link DocumentContext}, {@link DocumentFile}, {@link DocumentVersion}) the same way
 * {@link AuditMetadata} groups the audit columns - keeps the constructor at 7 parameters (master
 * standards doc binding rules, Section 13.1) instead of 13 loose ones.
 */
public final class Document {

    private final UUID id;
    private final DocumentContext context;
    private final DocumentFile file;
    private final DocumentStatus status;
    private final DocumentVersion version;
    private final Instant retentionUntil;
    private final AuditMetadata auditMetadata;

    public Document(UUID id, DocumentContext context, DocumentFile file, DocumentStatus status,
                     DocumentVersion version, Instant retentionUntil, AuditMetadata auditMetadata) {
        this.id = id;
        this.context = context;
        this.file = file;
        this.status = status;
        this.version = version;
        this.retentionUntil = retentionUntil;
        this.auditMetadata = auditMetadata;
    }

    public UUID getId() {
        return id;
    }

    public DocumentContext getContext() {
        return context;
    }

    public DocumentFile getFile() {
        return file;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public DocumentVersion getVersion() {
        return version;
    }

    public Instant getRetentionUntil() {
        return retentionUntil;
    }

    public AuditMetadata getAuditMetadata() {
        return auditMetadata;
    }
}
