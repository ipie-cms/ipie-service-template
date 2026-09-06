package in.gov.ipie.service.template.domain;

import java.util.UUID;

import lombok.Getter;

import in.gov.ipie.common.core.model.AuditMetadata;

/**
 * The User domain model - deliberately independent of the JPA entity in the infrastructure layer
 * (master standards doc, section 16: "Public APIs must not expose persistence entities", and more
 * generally the layers must stay independent so the persistence model can change without touching
 * business rules).
 *
 * <p>{@code @Getter} generates JavaBean-style getters (rather than record-style accessors) for
 * every field below, so {@code UserApiMapper}'s MapStruct interface can map this class
 * automatically by property name (see {@code UserApiMapper.toResponse}) - this class is also the
 * platform's one live combination of Lombok and MapStruct on the same type (master standards
 * doc, section 5's Lombok convention, "MapStruct interaction"), which is what that section's
 * {@code lombok-mapstruct-binding} guidance is tested against.
 */
@Getter
public final class User {

    private final UUID id;
    private final String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private UserStatus status;
    private final AuditMetadata auditMetadata;

    public User(UUID id, String username, String email, String fullName, String phoneNumber,
                UserStatus status, AuditMetadata auditMetadata) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.auditMetadata = auditMetadata;
    }

    public static User createNew(String username, String email, String fullName, String phoneNumber) {
        return new User(null, username, email, fullName, phoneNumber, UserStatus.ACTIVE, null);
    }

    public void updateDetails(String newEmail, String newFullName, String newPhoneNumber) {
        this.email = newEmail;
        this.fullName = newFullName;
        this.phoneNumber = newPhoneNumber;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void reactivate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
