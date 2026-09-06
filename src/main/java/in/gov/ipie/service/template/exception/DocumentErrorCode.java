package in.gov.ipie.service.template.exception;

import in.gov.ipie.common.core.exception.ErrorCode;

/** Stable, service-specific error codes for the Document domain (master standards doc, 5.4). */
public enum DocumentErrorCode implements ErrorCode {
    DOCUMENT_NOT_FOUND,
    SUPERSEDED_DOCUMENT_NOT_FOUND;

    @Override
    public String code() {
        return name();
    }
}
