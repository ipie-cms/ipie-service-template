package in.gov.ipie.service.template.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import in.gov.ipie.service.template.dto.request.CreateUserRequest;
import in.gov.ipie.service.template.dto.request.UpdateUserRequest;
import in.gov.ipie.service.template.dto.response.UserResponse;
import in.gov.ipie.service.template.command.CreateUserCommand;
import in.gov.ipie.service.template.command.UpdateUserCommand;
import in.gov.ipie.service.template.domain.User;

/** MapStruct mapping between the API's request/response DTOs and the application/domain model (master standards doc, 5.2). */
@Mapper(componentModel = "spring")
public interface UserApiMapper {

    CreateUserCommand toCommand(CreateUserRequest request);

    default UpdateUserCommand toCommand(UUID userId, UpdateUserRequest request) {
        return new UpdateUserCommand(userId, request.email(), request.fullName(), request.phoneNumber());
    }

    @Mapping(target = "createdAt", source = "auditMetadata.createdAt")
    @Mapping(target = "updatedAt", source = "auditMetadata.updatedAt")
    @Mapping(target = "version", source = "auditMetadata.version")
    UserResponse toResponse(User user);
    // Property lookup ("auditMetadata", "id", "username", ...) works because User exposes
    // JavaBean getters - see the comment on User's accessor methods.
}
