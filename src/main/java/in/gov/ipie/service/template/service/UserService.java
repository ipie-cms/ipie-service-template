package in.gov.ipie.service.template.service;

import java.util.UUID;

import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.template.command.CreateUserCommand;
import in.gov.ipie.service.template.command.UpdateUserCommand;
import in.gov.ipie.service.template.domain.User;
import in.gov.ipie.service.template.domain.UserSearchCriteria;

/**
 * User CRUD use cases. See {@link UserServiceImpl} for the implementation - the interface exists
 * so callers depend on a contract rather than a concrete class (e.g. for mocking in controller
 * tests, and so {@code UserValidationAspect}'s pointcuts target the implementation without the
 * interface itself carrying any AOP-specific concerns).
 */
public interface UserService {

    User createUser(CreateUserCommand command);

    User getUser(UUID userId);

    PageResult<User> searchUsers(UserSearchCriteria criteria, PageRequest pageRequest);

    /** Keyset ("seek") variant of {@link #searchUsers}; see {@code CursorPageRequest}'s Javadoc. */
    CursorPageResult<User> searchUsersAfter(UserSearchCriteria criteria, CursorPageRequest pageRequest);

    User updateUser(UpdateUserCommand command);

    User deactivateUser(UUID userId);

    User reactivateUser(UUID userId);
}
