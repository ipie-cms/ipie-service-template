package in.gov.ipie.service.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import in.gov.ipie.common.audit.AuditRecorder;
import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.common.events.outbox.OutboxStore;
import in.gov.ipie.common.filestorage.exception.FileTooLargeException;
import in.gov.ipie.common.filestorage.exception.MalwareDetectedException;
import in.gov.ipie.common.filestorage.exception.ScanUnavailableException;
import in.gov.ipie.common.filestorage.exception.UnsupportedFileTypeException;
import in.gov.ipie.common.filestorage.scanning.ScanResult;
import in.gov.ipie.common.filestorage.scanning.VirusScanner;
import in.gov.ipie.common.filestorage.storage.FileStorage;
import in.gov.ipie.common.filestorage.validation.AllowedFileType;
import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.service.template.command.UploadDocumentCommand;
import in.gov.ipie.service.template.exception.DocumentNotFoundException;
import in.gov.ipie.service.template.domain.Document;
import in.gov.ipie.service.template.domain.DocumentStatus;
import in.gov.ipie.service.template.repository.DocumentRepository;

class DocumentServiceImplTest {

    // Real PDF magic bytes so FileTypeValidator's Tika-based sniffing detects application/pdf.
    private static final byte[] PDF_BYTES = "%PDF-1.4\n%fake-but-detectable-pdf-content".getBytes(StandardCharsets.US_ASCII);
    private static final Set<AllowedFileType> ALLOWED = Set.of(AllowedFileType.PDF);

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final FileStorage fileStorage = mock(FileStorage.class);
    private final VirusScanner virusScanner = mock(VirusScanner.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentService = newService(26_214_400L);
        when(currentUserProvider.current()).thenReturn(Optional.empty());
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            AuditMetadata auditMetadata = new AuditMetadata(Instant.now(), "system", Instant.now(), "system", 0, true, null, null);
            return new Document(
                    UUID.randomUUID(), document.getContext(), document.getFile(), document.getStatus(),
                    document.getVersion(), document.getRetentionUntil(), auditMetadata);
        });
    }

    private DocumentServiceImpl newService(long maxFileSizeBytes) {
        DocumentUploadProperties properties = new DocumentUploadProperties("ipie-service-template-test", maxFileSizeBytes);
        return new DocumentServiceImpl(
                documentRepository, fileStorage, virusScanner, outboxStore, auditRecorder, currentUserProvider, properties);
    }

    @Test
    void uploadDocument_savesAsCleanAndPromotesToPermanentStorage_whenScanIsClean() {
        when(virusScanner.scan(any(), anyLong())).thenReturn(ScanResult.clean());
        UploadDocumentCommand command = new UploadDocumentCommand("case-1", "court-order", "order.pdf", PDF_BYTES, null);

        Document result = documentService.uploadDocument(command, ALLOWED);

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.CLEAN);
        verify(fileStorage).put(any(), any(), eq((long) PDF_BYTES.length), any());
        verify(fileStorage).copy(any(), any());
        verify(fileStorage).delete(any());
        verify(outboxStore).save(any());
    }

    @Test
    void uploadDocument_throwsMalwareDetected_andDeletesQuarantinedBytes_whenScanIsInfected() {
        when(virusScanner.scan(any(), anyLong()))
                .thenReturn(ScanResult.infected("Eicar-Test-Signature"));
        UploadDocumentCommand command = new UploadDocumentCommand("case-1", "court-order", "order.pdf", PDF_BYTES, null);

        assertThatThrownBy(() -> documentService.uploadDocument(command, ALLOWED))
                .isInstanceOf(MalwareDetectedException.class);

        verify(fileStorage).delete(any());
        verify(fileStorage, never()).copy(any(), any());
        verify(auditRecorder).record(any());
    }

    @Test
    void uploadDocument_throwsScanUnavailable_whenScanErrors() {
        when(virusScanner.scan(any(), anyLong())).thenReturn(ScanResult.error("unreachable"));
        UploadDocumentCommand command = new UploadDocumentCommand("case-1", "court-order", "order.pdf", PDF_BYTES, null);

        assertThatThrownBy(() -> documentService.uploadDocument(command, ALLOWED))
                .isInstanceOf(ScanUnavailableException.class);

        verify(fileStorage).delete(any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_throwsUnsupportedFileType_whenContentDoesNotMatchWhitelist_andNeverTouchesStorage() {
        byte[] plainText = "not a pdf at all".getBytes(StandardCharsets.UTF_8);
        UploadDocumentCommand command = new UploadDocumentCommand("case-1", "court-order", "order.pdf", plainText, null);

        assertThatThrownBy(() -> documentService.uploadDocument(command, ALLOWED))
                .isInstanceOf(UnsupportedFileTypeException.class);

        verify(fileStorage, never()).put(any(), any(), anyLong(), any());
    }

    @Test
    void uploadDocument_throwsFileTooLarge_whenOverTheConfiguredLimit() {
        DocumentServiceImpl tinyLimitService = newService(10L);
        UploadDocumentCommand command = new UploadDocumentCommand("case-1", "court-order", "order.pdf", PDF_BYTES, null);

        assertThatThrownBy(() -> tinyLimitService.uploadDocument(command, ALLOWED))
                .isInstanceOf(FileTooLargeException.class);
    }

    @Test
    void getDocument_throwsNotFound_whenMissing() {
        UUID missingId = UUID.randomUUID();
        when(documentRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(missingId)).isInstanceOf(DocumentNotFoundException.class);
    }
}
