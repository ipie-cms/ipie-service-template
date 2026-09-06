package in.gov.ipie.service.template.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(max = 200)
        String fullName,

        @Pattern(regexp = "^[+0-9 ()-]{0,20}$", message = "must be a valid phone number")
        String phoneNumber) {
}
