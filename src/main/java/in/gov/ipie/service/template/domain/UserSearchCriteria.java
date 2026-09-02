package in.gov.ipie.service.template.domain;

/** Optional filters for listing users; any field left {@code null} is not applied. */
public record UserSearchCriteria(String usernameContains, String emailContains, UserStatus status) {

    public static UserSearchCriteria empty() {
        return new UserSearchCriteria(null, null, null);
    }
}
