package in.gov.ipie.service.template.service;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.common.audit.AuditRecorder;
import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEvent;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.outbox.OutboxStore;
import in.gov.ipie.common.filestorage.exception.MalwareDetectedException;
import in.gov.ipie.common.filestorage.exception.ScanUnavailableException;
import in.gov.ipie.common.filestorage.hash.FileHasher;
import in.gov.ipie.common.filestorage.naming.StorageKeyGenerator;
import in.gov.ipie.common.filestorage.scanning.ScanResult;
import in.gov.ipie.common.filestorage.scanning.ScanStatus;
import in.gov.ipie.common.filestorage.scanning.VirusScanner;
import in.gov.ipie.common.filestorage.storage.FileStorage;
import in.gov.ipie.common.filestorage.validation.AllowedFileType;
import in.gov.ipie.common.filestorage.validation.FileSizeValidator;
import in.gov.ipie.common.filestorage.validation.FileTypeValidator;
import in.gov.ipie.common.observability.correlation.LoggingContext;
import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.service.template.command.UploadDocumentCommand;
import in.gov.ipie.service.template.event.DocumentEventType;
import in.gov.ipie.service.template.exception.DocumentNotFoundException;
import in.gov.ipie.service.template.domain.Document;
import in.gov.ipie.service.template.domain.DocumentContext;
import in.gov.ipie.service.template.domain.DocumentFile;
import in.gov.ipie.service.template.domain.DocumentStatus;
import in.gov.ipie.service.template.domain.DocumentVersion;
import in.gov.ipie.service.template.repository.DocumentRepository;

/**
 * Orchestrates the full quarantine-first upload pipeline (master standards doc, file-upload
 * rules): validate type/size -&gt; write to quarantine -&gt; scan -&gt; promote to permanent
 * storage on a clean result (or discard on infected/unscannable) -&gt; persist metadata -&gt;
 * audit -&gt; outbox event. No caller of this service ever touches {@code FileStorage} or {@code
 * VirusScanner} directly.
 *
 * <p><b>Not yet built here</b> (see the master standards doc's gaps list): full OPA-based ABAC
 * (case-association checks, not just the permission check {@code DocumentController} already
 * does), gateway-level size limits, and automatic retention-driven purge - {@link
 * #retentionUntilFor} only computes the column value from configuration, it does not enforce
 * deletion.
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final FileTypeValidator FILE_TYPE_VALIDATOR = new FileTypeValidator();

    private final DocumentRepository documentRepository;
    private final FileStorage fileStorage;
    private final VirusScanner virusScanner;
    private final OutboxStore outboxStore;
    private final AuditRecorder auditRecorder;
    private final CurrentUserProvider currentUserProvider;
    private final DocumentUploadProperties properties;

    public DocumentServiceImpl(
            DocumentRepository documentRepository,
            FileStorage fileStorage,
            VirusScanner virusScanner,
            OutboxStore outboxStore,
            AuditRecorder auditRecorder,
            CurrentUserProvider currentUserProvider,
            DocumentUploadProperties properties) {
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
        this.virusScanner = virusScanner;
        this.outboxStore = outboxStore;
        this.auditRecorder = auditRecorder;
        this.currentUserProvider = currentUserProvider;
        this.properties = properties;
    }

    @Override
    @Transactional
    @Auditable(action = "DOCUMENT_UPLOADED", entityType = "DOCUMENT", entityId = "#result.id",
            caseId = "#command.caseId()", eventType = AuditEventType.BUSINESS)
    public Document uploadDocument(UploadDocumentCommand command, Set<AllowedFileType> allowedTypes) {
        // Read once - UploadDocumentCommand.content() defensively clones on every call (it holds
        // a mutable byte[]), so capturing it once here avoids repeatedly cloning a potentially
        // large array for every subsequent use below.
        byte[] content = command.content();

        FileSizeValidator.validate(content.length, properties.maxFileSizeBytes());
        FILE_TYPE_VALIDATOR.validate(content, allowedTypes);

        DocumentContext context = new DocumentContext(command.caseId(), command.docType());
        String extension = extensionOf(command.originalFilename());
        String objectPath = StorageKeyGenerator.newObjectPath(properties.serviceName(), command.caseId(), command.docType(), extension);
        String quarantineKey = StorageKeyGenerator.quarantineKey(objectPath);
        String sha256Hash = FileHasher.sha256Hex(content);
        String contentType = FILE_TYPE_VALIDATOR.detectMimeType(content);

        fileStorage.put(quarantineKey, new ByteArrayInputStream(content), content.length, contentType);

        ScanResult scanResult = virusScanner.scan(new ByteArrayInputStream(content), content.length);

        if (scanResult.status() == ScanStatus.ERROR) {
            fileStorage.delete(quarantineKey);
            throw new ScanUnavailableException();
        }
        if (scanResult.status() == ScanStatus.INFECTED) {
            fileStorage.delete(quarantineKey);
            DocumentFile rejectedFile = new DocumentFile(
                    command.originalFilename(), null, contentType, content.length, sha256Hash);
            DocumentVersion version = nextVersion(command.supersedesId());
            Document rejected = new Document(
                    null, context, rejectedFile, DocumentStatus.INFECTED, version, null, null);
            Document saved = documentRepository.save(rejected);
            enqueueEvent(DocumentEventType.DOCUMENT_UPLOAD_REJECTED, saved);
            recordSecurityAudit("DOCUMENT_UPLOAD_REJECTED_MALWARE", saved, scanResult.detail());
            throw new MalwareDetectedException();
        }

        String permanentKey = StorageKeyGenerator.permanentKey(objectPath);
        fileStorage.copy(quarantineKey, permanentKey);
        fileStorage.delete(quarantineKey);

        DocumentFile cleanFile = new DocumentFile(
                command.originalFilename(), permanentKey, contentType, content.length, sha256Hash);
        DocumentVersion version = nextVersion(command.supersedesId());
        Document clean = new Document(
                null, context, cleanFile, DocumentStatus.CLEAN, version, retentionUntilFor(command.docType()), null);
        Document saved = documentRepository.save(clean);
        enqueueEvent(DocumentEventType.DOCUMENT_UPLOADED, saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Document getDocument(UUID id) {
        return documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> getDocumentsForCase(String caseId) {
        return documentRepository.findByCaseId(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public String downloadUrl(UUID id, Duration expiry) {
        Document document = getDocument(id);
        if (document.getFile().storageKey() == null) {
            throw new DocumentNotFoundException(id);
        }
        return fileStorage.presignedDownloadUrl(document.getFile().storageKey(), expiry);
    }

    private DocumentVersion nextVersion(UUID supersedesId) {
        if (supersedesId == null) {
            return new DocumentVersion(1, null);
        }
        int previousNumber = documentRepository.findById(supersedesId)
                .map(previous -> previous.getVersion().number())
                .orElseThrow(() -> new DocumentNotFoundException(supersedesId));
        return new DocumentVersion(previousNumber + 1, supersedesId);
    }

    /**
     * {@code null} means no retention policy is configured for this doc type yet - real statutory
     * retention periods for IBC-related records need legal/compliance confirmation before a
     * default is set here (master standards doc's gaps list).
     */
    private Instant retentionUntilFor(String docType) {
        return null;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private void enqueueEvent(DocumentEventType eventType, Document document) {
        EventEnvelope<UUID> event = EventEnvelope.create(
                eventType.name(), DocumentEventType.CONTRACT_VERSION, properties.serviceName(),
                LoggingContext.correlationId(), document.getContext().caseId(), document.getId());
        outboxStore.save(event);
    }

    /**
     * {@code @Auditable} only records an event after its method returns successfully, so a
     * malware-detected upload (which throws) needs its own explicit {@link AuditEvent} - a
     * {@link AuditEventType#SECURITY} record, not {@code BUSINESS}, since a rejected malicious
     * upload is a security-relevant fact, not just a business one.
     */
    private void recordSecurityAudit(String action, Document document, String scanDetail) {
        AuditEvent event = new AuditEvent(
                AuditEventType.SECURITY,
                action,
                "DOCUMENT",
                document.getId() != null ? document.getId().toString() : null,
                document.getContext().caseId(),
                currentUserProvider.current().map(user -> user.userId()).orElse("system"),
                null,
                properties.serviceName(),
                null,
                null,
                scanDetail,
                LoggingContext.correlationId(),
                Instant.now());
        auditRecorder.record(event);
    }
}
