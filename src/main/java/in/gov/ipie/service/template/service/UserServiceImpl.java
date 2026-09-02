package in.gov.ipie.service.template.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.outbox.OutboxStore;
import in.gov.ipie.common.observability.correlation.LoggingContext;
import in.gov.ipie.service.template.command.CreateUserCommand;
import in.gov.ipie.service.template.command.UpdateUserCommand;
import in.gov.ipie.service.template.exception.UserNotFoundException;
import in.gov.ipie.service.template.domain.User;
import in.gov.ipie.service.template.domain.UserSearchCriteria;
import in.gov.ipie.service.template.repository.UserRepository;
import in.gov.ipie.service.template.repository.UserSearchIndex;
import in.gov.ipie.service.template.event.UserEventType;

/**
 * {@link UserService} implementation. Business rules (state transitions) live here and in the
 * domain model - controllers only translate HTTP <-> commands (master standards doc, 5.1/5.2:
 * "Keep controllers thin"). Username/email uniqueness validation lives in
 * {@link UserValidationAspect}, not here - see its Javadoc.
 *
 * <p>Events go through {@link OutboxStore}, never straight to {@code EventPublisher} - writing the
 * outbox row inside the same {@code @Transactional} boundary as the entity save is what makes the
 * two atomic (master standards doc, section 9). {@code OutboxRelayScheduler} is the only thing
 * that talks to the real publisher.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserSearchIndex userSearchIndex;
    private final OutboxStore outboxStore;
    private final String serviceName;

    public UserServiceImpl(
            UserRepository userRepository,
            UserSearchIndex userSearchIndex,
            OutboxStore outboxStore,
            @Value("${spring.application.name}") String serviceName) {
        this.userRepository = userRepository;
        this.userSearchIndex = userSearchIndex;
        this.outboxStore = outboxStore;
        this.serviceName = serviceName;
    }

    @Override
    @Transactional
    @Auditable(action = "USER_CREATED", entityType = "USER", entityId = "#result.id", eventType = AuditEventType.BUSINESS)
    public User createUser(CreateUserCommand command) {
        User user = User.createNew(command.username(), command.email(), command.fullName(), command.phoneNumber());
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        enqueueEvent(UserEventType.USER_CREATED, saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> searchUsers(UserSearchCriteria criteria, PageRequest pageRequest) {
        return userSearchIndex.search(criteria, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResult<User> searchUsersAfter(UserSearchCriteria criteria, CursorPageRequest pageRequest) {
        return userSearchIndex.searchAfter(criteria, pageRequest);
    }

    @Override
    @Transactional
    @Auditable(action = "USER_UPDATED", entityType = "USER", entityId = "#command.userId()")
    public User updateUser(UpdateUserCommand command) {
        User user = userRepository.findById(command.userId()).orElseThrow(() -> new UserNotFoundException(command.userId()));
        user.updateDetails(command.email(), command.fullName(), command.phoneNumber());
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        enqueueEvent(UserEventType.USER_UPDATED, saved);
        return saved;
    }

    @Override
    @Transactional
    @Auditable(action = "USER_DEACTIVATED", entityType = "USER", entityId = "#userId")
    public User deactivateUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.deactivate();
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        enqueueEvent(UserEventType.USER_DEACTIVATED, saved);
        return saved;
    }

    @Override
    @Transactional
    @Auditable(action = "USER_REACTIVATED", entityType = "USER", entityId = "#userId")
    public User reactivateUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.reactivate();
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        enqueueEvent(UserEventType.USER_REACTIVATED, saved);
        return saved;
    }

    private void enqueueEvent(UserEventType eventType, User user) {
        EventEnvelope<UUID> event = EventEnvelope.create(
                eventType.name(), UserEventType.CONTRACT_VERSION, serviceName, LoggingContext.correlationId(), null, user.getId());
        outboxStore.save(event);
    }
}
