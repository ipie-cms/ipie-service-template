package in.gov.ipie.service.template.dto.response;

import java.time.Instant;

import in.gov.ipie.service.template.domain.UserStatus;

public record UserResponse(
        String id,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
