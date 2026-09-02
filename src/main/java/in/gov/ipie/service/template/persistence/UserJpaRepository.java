package in.gov.ipie.service.template.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


/**
 * Public so {@code UserRepositoryImpl} (the {@code repositoryimpl} sibling subpackage) can use
 * it - by convention, no other class should reference this interface.
 */
public interface UserJpaRepository
        extends JpaRepository<UserJpaEntity, UUID>, JpaSpecificationExecutor<UserJpaEntity>, UserJpaRepositoryCustom {

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID excludedId);
}
