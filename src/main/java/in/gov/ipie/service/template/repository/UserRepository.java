package in.gov.ipie.service.template.repository;

import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.template.domain.User;
import in.gov.ipie.service.template.domain.UserSearchCriteria;

/**
 * Domain-owned port for User persistence. The application layer depends only on this interface -
 * the JPA-backed implementation lives in the infrastructure layer (master standards doc, 5.1:
 * "Controllers should not call repositories directly" / section 16 layering rules).
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    /** Same email check as {@link #existsByEmailIgnoreCase(String)} but excluding one user, for update flows. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID excludedId);

    PageResult<User> search(UserSearchCriteria criteria, PageRequest pageRequest);

    /**
     * Keyset ("seek") variant of {@link #search} - ordered by {@code (createdAt, id)} ascending,
     * no {@code COUNT(*)}. Prefer this over {@link #search} for large/high-traffic listings; see
     * {@code CursorPageRequest}'s Javadoc.
     */
    CursorPageResult<User> searchAfter(UserSearchCriteria criteria, CursorPageRequest pageRequest);
}
