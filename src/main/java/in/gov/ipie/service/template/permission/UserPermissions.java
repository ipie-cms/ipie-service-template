package in.gov.ipie.service.template.permission;

/**
 * Permission names for the User API, enforced via {@code PermissionEnforcer.require(...)}.
 * Business logic must never check role names directly (master standards doc, 5.5) - only these
 * permission constants, resolved from the caller's JWT by common-security.
 */
public final class UserPermissions {

    public static final String USER_READ = "USER_READ";
    public static final String USER_WRITE = "USER_WRITE";
    public static final String USER_DELETE = "USER_DELETE";

    private UserPermissions() {
    }
}
