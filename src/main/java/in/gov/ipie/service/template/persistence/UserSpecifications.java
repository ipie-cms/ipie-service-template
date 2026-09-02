package in.gov.ipie.service.template.persistence;

import org.springframework.data.jpa.domain.Specification;

import in.gov.ipie.service.template.domain.UserSearchCriteria;

/**
 * Public so the {@code repository} subpackage - the only other caller - can use it across the
 * package boundary.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<UserJpaEntity> matching(UserSearchCriteria criteria) {
        Specification<UserJpaEntity> specification = (root, query, cb) -> cb.conjunction();

        if (criteria.usernameContains() != null && !criteria.usernameContains().isBlank()) {
            String pattern = "%" + criteria.usernameContains().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("username")), pattern));
        }
        if (criteria.emailContains() != null && !criteria.emailContains().isBlank()) {
            String pattern = "%" + criteria.emailContains().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), pattern));
        }
        if (criteria.status() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), criteria.status()));
        }

        return specification;
    }
}
