package in.gov.ipie.service.template.exception;

import in.gov.ipie.common.core.exception.ConflictException;

public class UsernameAlreadyExistsException extends ConflictException {

    public UsernameAlreadyExistsException(String username) {
        super(UserErrorCode.USERNAME_ALREADY_EXISTS, "Username '" + username + "' is already taken");
    }
}
