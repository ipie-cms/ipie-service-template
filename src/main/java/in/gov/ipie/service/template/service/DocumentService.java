package in.gov.ipie.service.template.service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import in.gov.ipie.common.filestorage.validation.AllowedFileType;
import in.gov.ipie.service.template.command.UploadDocumentCommand;
import in.gov.ipie.service.template.domain.Document;

/**
 * Document upload/retrieval use cases (the quarantine-first pipeline). See
 * {@link DocumentServiceImpl} for the implementation - the interface exists so callers depend on
 * a contract rather than a concrete class.
 */
public interface DocumentService {

    Document uploadDocument(UploadDocumentCommand command, Set<AllowedFileType> allowedTypes);

    Document getDocument(UUID id);

    List<Document> getDocumentsForCase(String caseId);

    /** A short-lived signed URL - the client downloads directly from storage, never proxied through this service. */
    String downloadUrl(UUID id, Duration expiry);
}
