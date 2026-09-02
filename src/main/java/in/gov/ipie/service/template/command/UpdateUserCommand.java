package in.gov.ipie.service.template.command;

import java.util.UUID;

public record UpdateUserCommand(UUID userId, String email, String fullName, String phoneNumber) {
}
