package in.gov.ipie.service.template.exception;

import java.util.UUID;

import in.gov.ipie.common.core.exception.NotFoundException;

public class DocumentNotFoundException extends NotFoundException {

    public DocumentNotFoundException(UUID documentId) {
        super(DocumentErrorCode.DOCUMENT_NOT_FOUND, "Document " + documentId + " was not found");
    }
}
