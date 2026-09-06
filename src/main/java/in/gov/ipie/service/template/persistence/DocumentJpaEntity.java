package in.gov.ipie.service.template.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import in.gov.ipie.common.utils.id.UuidV7Generator;

import in.gov.ipie.common.persistence.AuditableJpaEntity;
import in.gov.ipie.service.template.domain.DocumentContext;
import in.gov.ipie.service.template.domain.DocumentFile;
import in.gov.ipie.service.template.domain.DocumentStatus;
import in.gov.ipie.service.template.domain.DocumentVersion;

/**
 * JPA representation of a document's metadata (never the file bytes - see {@code FileStorage}).
 * Never returned from an API and never referenced outside infrastructure - {@code
 * DocumentPersistenceMapper} converts to/from the domain {@code Document} at the repository
 * boundary. Rows here are never updated after creation (a re-upload is a new row via {@code
 * supersedesId}, not an overwrite - master standards doc, file-upload rules, section 8).
 *
 * <p>Columns stay flat/individual (unlike the domain model, which groups them into {@link
 * DocumentContext}/{@link DocumentFile}/{@link DocumentVersion}) - the constructor accepts the
 * same grouped parameters purely to stay under the 7-parameter binding-rule limit (master
 * standards doc, Section 13.1), then unpacks them into individual columns internally. Standard
 * audit + soft-delete columns are inherited from {@link AuditableJpaEntity} (master standards
 * doc, 7.2).
 */
@Entity
@Table(name = "documents")
public class DocumentJpaEntity extends AuditableJpaEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    @Column(name = "case_id", nullable = false, length = 100)
    private String caseId;

    @Column(name = "doc_type", nullable = false, length = 100)
    private String docType;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_key", length = 512)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentStatus status;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "supersedes_id")
    private UUID supersedesId;

    @Column(name = "retention_until")
    private Instant retentionUntil;

    protected DocumentJpaEntity() {
        // required by JPA
    }

    public DocumentJpaEntity(UUID id, DocumentContext context, DocumentFile file, DocumentStatus status,
                              DocumentVersion version, Instant retentionUntil) {
        this.id = id;
        this.caseId = context.caseId();
        this.docType = context.docType();
        this.originalFilename = file.originalFilename();
        this.storageKey = file.storageKey();
        this.contentType = file.contentType();
        this.sizeBytes = file.sizeBytes();
        this.sha256Hash = file.sha256Hash();
        this.status = status;
        this.versionNumber = version.number();
        this.supersedesId = version.supersedesId();
        this.retentionUntil = retentionUntil;
    }

    public UUID getId() {
        return id;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getDocType() {
        return docType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public UUID getSupersedesId() {
        return supersedesId;
    }

    public Instant getRetentionUntil() {
        return retentionUntil;
    }
}
