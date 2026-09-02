package in.gov.ipie.service.template.service;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import in.gov.ipie.service.template.command.CreateUserCommand;
import in.gov.ipie.service.template.command.UpdateUserCommand;
import in.gov.ipie.service.template.exception.EmailAlreadyExistsException;
import in.gov.ipie.service.template.exception.UsernameAlreadyExistsException;
import in.gov.ipie.service.template.repository.UserRepository;

/**
 * Username/email uniqueness validation for {@link UserServiceImpl}, run before the method body
 * via AspectJ pointcuts rather than inline {@code if}/{@code throw} checks at the top of the
 * service method - keeps {@link UserServiceImpl} focused on the write itself.
 *
 * <p>{@code validateUpdateUser} calls {@code existsByEmailIgnoreCaseAndIdNot} unconditionally,
 * skipping the "is the email actually changing" short-circuit the old inline check had - that
 * check was a pure query-avoidance optimization, not a correctness requirement:
 * {@code existsByEmailIgnoreCaseAndIdNot} already excludes the user's own row, so an unchanged
 * email can never match another row anyway.
 */
@Aspect
@Component
class UserValidationAspect {

    private final UserRepository userRepository;

    UserValidationAspect(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Before("execution(* in.gov.ipie.service.template.service.UserServiceImpl.createUser(..)) && args(command)")
    void validateCreateUser(CreateUserCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new UsernameAlreadyExistsException(command.username());
        }
        if (userRepository.existsByEmailIgnoreCase(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }
    }

    @Before("execution(* in.gov.ipie.service.template.service.UserServiceImpl.updateUser(..)) && args(command)")
    void validateUpdateUser(UpdateUserCommand command) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(command.email(), command.userId())) {
            throw new EmailAlreadyExistsException(command.email());
        }
    }
}
