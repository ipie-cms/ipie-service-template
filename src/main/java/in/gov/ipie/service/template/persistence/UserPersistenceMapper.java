package in.gov.ipie.service.template.persistence;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.service.template.domain.User;

/**
 * Converts between the JPA entity and the domain model. A hand-written mapper (rather than
 * MapStruct) because it also assembles the {@link AuditMetadata} value object from five separate
 * entity columns - see {@code UserApiMapper} for the MapStruct-based mapping the master standards
 * doc calls for (5.2: "Use MapStruct for complex mappings") between API DTOs and the domain model.
 * Public so the {@code repository} subpackage - the only other caller - can use it across the
 * package boundary.
 */
@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        AuditMetadata auditMetadata = new AuditMetadata(
                entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getVersion(),
                entity.isActive(), entity.getDeletedAt(), entity.getDeletedBy());
        return new User(
                entity.getId(), entity.getUsername(), entity.getEmail(), entity.getFullName(),
                entity.getPhoneNumber(), entity.getStatus(), auditMetadata);
    }

    public UserJpaEntity toNewEntity(User user) {
        return new UserJpaEntity(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getPhoneNumber(), user.getStatus());
    }

    public void copyMutableFieldsOnto(User user, UserJpaEntity entity) {
        entity.setEmail(user.getEmail());
        entity.setFullName(user.getFullName());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setStatus(user.getStatus());
    }
}
