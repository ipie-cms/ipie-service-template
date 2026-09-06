package in.gov.ipie.service.template.exception;

import in.gov.ipie.common.core.exception.ErrorCode;

/** Stable, service-specific error codes for the User domain (master standards doc, 5.4). */
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND,
    USERNAME_ALREADY_EXISTS,
    EMAIL_ALREADY_EXISTS;

    @Override
    public String code() {
        return name();
    }
}
