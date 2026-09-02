package in.gov.ipie.service.template.command;

public record CreateUserCommand(String username, String email, String fullName, String phoneNumber) {
}
