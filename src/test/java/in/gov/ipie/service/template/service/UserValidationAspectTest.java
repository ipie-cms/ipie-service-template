package in.gov.ipie.service.template.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.core.exception.ConflictException;
import in.gov.ipie.service.template.command.CreateUserCommand;
import in.gov.ipie.service.template.command.UpdateUserCommand;
import in.gov.ipie.service.template.exception.EmailAlreadyExistsException;
import in.gov.ipie.service.template.exception.UsernameAlreadyExistsException;
import in.gov.ipie.service.template.repository.UserRepository;

class UserValidationAspectTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserValidationAspect aspect = new UserValidationAspect(userRepository);

    @Test
    void validateCreateUser_throwsConflict_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("jdoe")).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateCreateUser(new CreateUserCommand("jdoe", "jdoe@example.com", "Jane Doe", null)))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void validateCreateUser_throwsConflict_whenEmailAlreadyExists() {
        when(userRepository.existsByUsername("jdoe")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("jdoe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateCreateUser(new CreateUserCommand("jdoe", "jdoe@example.com", "Jane Doe", null)))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void validateCreateUser_passes_whenUsernameAndEmailAreUnique() {
        assertThatCode(() -> aspect.validateCreateUser(new CreateUserCommand("jdoe", "jdoe@example.com", "Jane Doe", null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateUpdateUser_throwsConflict_whenEmailAlreadyExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", userId)).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateUpdateUser(new UpdateUserCommand(userId, "taken@example.com", "Jane Doe", null)))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void validateUpdateUser_passes_whenEmailIsAvailable() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("jdoe@example.com", userId)).thenReturn(false);

        assertThatCode(() -> aspect.validateUpdateUser(new UpdateUserCommand(userId, "jdoe@example.com", "Jane Doe", null)))
                .doesNotThrowAnyException();
    }
}
