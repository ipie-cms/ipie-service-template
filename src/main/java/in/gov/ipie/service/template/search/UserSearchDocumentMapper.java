package in.gov.ipie.service.template.search;

import java.util.UUID;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.service.template.domain.User;
import in.gov.ipie.service.template.domain.UserStatus;

/**
 * Converts between the domain {@code User} and its {@link UserDocument} projection. Public so the
 * {@code searchindex} sibling subpackage and {@code UserSearchIndexConfig} can use it across the
 * package boundary.
 */
@Component
public class UserSearchDocumentMapper {

    public UserDocument toDocument(User user) {
        return new UserDocument(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getStatus().name(),
                user.getAuditMetadata());
    }

    public User toDomain(UserDocument document) {
        AuditMetadata auditMetadata = new AuditMetadata(
                document.getCreatedAt(), document.getCreatedBy(), document.getUpdatedAt(), document.getUpdatedBy(),
                document.getVersion(), document.isActive(), document.getDeletedAt(), document.getDeletedBy());
        return new User(
                UUID.fromString(document.getId()), document.getUsername(), document.getEmail(),
                document.getFullName(), document.getPhoneNumber(), UserStatus.valueOf(document.getStatus()),
                auditMetadata);
    }
}
