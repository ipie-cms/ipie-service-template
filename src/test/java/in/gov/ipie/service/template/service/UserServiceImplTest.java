package in.gov.ipie.service.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.events.outbox.OutboxStore;
import in.gov.ipie.service.template.command.CreateUserCommand;
import in.gov.ipie.service.template.exception.UserNotFoundException;
import in.gov.ipie.service.template.domain.User;
import in.gov.ipie.service.template.domain.UserSearchCriteria;
import in.gov.ipie.service.template.domain.UserStatus;
import in.gov.ipie.service.template.repository.UserRepository;
import in.gov.ipie.service.template.repository.UserSearchIndex;

/** Username/email uniqueness is validated by {@code UserValidationAspect} - see {@code UserValidationAspectTest}, not here. */
class UserServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserSearchIndex userSearchIndex = mock(UserSearchIndex.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userSearchIndex, outboxStore, "ipie-service-template-test");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
            AuditMetadata auditMetadata = new AuditMetadata(Instant.now(), "system", Instant.now(), "system", 0, true, null, null);
            return new User(
                    id, user.getUsername(), user.getEmail(), user.getFullName(), user.getPhoneNumber(),
                    user.getStatus(), auditMetadata);
        });
    }

    @Test
    void createUser_savesAndPublishesEvent() {
        User created = userService.createUser(new CreateUserCommand("jdoe", "jdoe@example.com", "Jane Doe", null));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userSearchIndex).index(created);
        verify(outboxStore).save(any());
    }

    @Test
    void getUser_throwsNotFound_whenMissing() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(missingId)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void searchUsersAfter_delegatesToUserSearchIndex() {
        CursorPageRequest pageRequest = CursorPageRequest.firstPage(20);
        CursorPageResult<User> expected = CursorPageResult.of(List.of(), null, false);
        when(userSearchIndex.searchAfter(UserSearchCriteria.empty(), pageRequest)).thenReturn(expected);

        CursorPageResult<User> result = userService.searchUsersAfter(UserSearchCriteria.empty(), pageRequest);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void deactivateUser_flipsStatusToInactive() {
        UUID userId = UUID.randomUUID();
        AuditMetadata auditMetadata = new AuditMetadata(Instant.now(), "system", Instant.now(), "system", 0, true, null, null);
        User activeUser = new User(userId, "jdoe", "jdoe@example.com", "Jane Doe", null, UserStatus.ACTIVE, auditMetadata);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        User deactivated = userService.deactivateUser(userId);

        assertThat(deactivated.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(outboxStore).save(any());
    }
}
