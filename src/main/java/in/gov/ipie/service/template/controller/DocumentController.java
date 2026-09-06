package in.gov.ipie.service.template.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.service.template.permission.DocumentPermissions;
import in.gov.ipie.service.template.configuration.DocumentTypeWhitelist;
import in.gov.ipie.service.template.mapper.DocumentApiMapper;
import in.gov.ipie.service.template.dto.response.DocumentResponse;
import in.gov.ipie.service.template.command.UploadDocumentCommand;
import in.gov.ipie.service.template.service.DocumentService;
import in.gov.ipie.service.template.domain.Document;

/**
 * Document upload API - the reference implementation of the master standards doc's file-upload
 * rules end to end. Only HTTP/multipart concerns live here; the pipeline itself (validate,
 * quarantine, scan, promote, persist, audit, publish) is entirely in {@link DocumentService}
 * (master standards doc, 5.1/5.2: "Keep controllers thin"). Permission checks are AOP advice
 * ({@link RequiresPermission}), not inline code - see {@code PermissionCheckAspect}.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private static final Duration DOWNLOAD_URL_EXPIRY = Duration.ofMinutes(15);

    private final DocumentService documentService;
    private final DocumentApiMapper documentApiMapper;

    public DocumentController(DocumentService documentService, DocumentApiMapper documentApiMapper) {
        this.documentService = documentService;
        this.documentApiMapper = documentApiMapper;
    }

    @PostMapping
    @RequiresPermission(DocumentPermissions.DOCUMENT_WRITE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam String caseId,
            @RequestParam String docType,
            @RequestParam(required = false) UUID supersedesId) {
        UploadDocumentCommand command = new UploadDocumentCommand(
                caseId, docType, file.getOriginalFilename(), readBytes(file), supersedesId);
        Document uploaded = documentService.uploadDocument(command, DocumentTypeWhitelist.forDocType(docType));
        DocumentResponse response = documentApiMapper.toResponse(uploaded);

        return ResponseEntity.created(URI.create("/api/v1/documents/" + uploaded.getId())).body(response);
    }

    @GetMapping("/{id}")
    @RequiresPermission(DocumentPermissions.DOCUMENT_READ)
    public DocumentResponse getDocument(@PathVariable UUID id) {
        return documentApiMapper.toResponse(documentService.getDocument(id));
    }

    @GetMapping
    @RequiresPermission(DocumentPermissions.DOCUMENT_READ)
    public List<DocumentResponse> getDocumentsForCase(@RequestParam String caseId) {
        return documentService.getDocumentsForCase(caseId).stream().map(documentApiMapper::toResponse).toList();
    }

    /** Returns a short-lived signed URL - the client downloads directly from storage, never proxied through this service. */
    @GetMapping("/{id}/download-url")
    @RequiresPermission(DocumentPermissions.DOCUMENT_READ)
    public Map<String, Object> downloadUrl(@PathVariable UUID id) {
        String url = documentService.downloadUrl(id, DOWNLOAD_URL_EXPIRY);
        return Map.of("url", url, "expiresInSeconds", DOWNLOAD_URL_EXPIRY.toSeconds());
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}
