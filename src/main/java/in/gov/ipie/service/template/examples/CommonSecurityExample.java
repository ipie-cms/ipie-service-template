package in.gov.ipie.service.template.examples;

import java.util.Optional;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.security.context.CurrentUser;
import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.common.security.password.PasswordPolicy;
import in.gov.ipie.common.security.permission.PermissionAuthorities;
import in.gov.ipie.common.security.permission.PermissionEnforcer;
import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.common.security.secret.DigestSecretHasher;
import in.gov.ipie.common.security.secret.SecretGenerator;
import in.gov.ipie.common.security.secret.SecretHasher;
import in.gov.ipie.service.template.permission.UserPermissions;

/**
 * Example-only reference code for {@code common-security} - not wired into any real request flow.
 * {@code UserController}/{@code DocumentController} use {@code @RequiresPermission} for real and
 * {@code DocumentServiceImpl} uses {@link CurrentUserProvider}; this class adds the parts of the
 * package the template's own flow never reaches - the programmatic gate, the authority-string
 * helper, and secret generation/hashing.
 *
 * <p><b>Check permissions, never role names.</b> A role is an assignment decision that changes per
 * deployment; a permission is what the code actually requires. Business logic that tests for
 * {@code "ADMIN"} has to be edited every time the role model moves (master standards doc, 5.5).
 *
 * <p><b>Do not reach for {@code SecurityContextHolder}.</b> {@link CurrentUserProvider} is a port,
 * so a unit test supplies a caller by implementing one interface method instead of installing a
 * security context - and the business code stops depending on Spring Security at all.
 *
 * <p><b>Rate limiting, HMAC request signing and the Keycloak clients are configuration, not code.</b>
 * {@code RateLimitFilter}, {@code HmacSignatureVerificationFilter}, {@code KeycloakTokenClient} and
 * {@code KeycloakUserManagementClient} are wired by their own auto-configurations from
 * {@code ipie.*} properties; a service turns them on rather than calling them, which is why none
 * of them appears below.
 */
@Component
public class CommonSecurityExample {

    private final CurrentUserProvider currentUserProvider;
    private final PermissionEnforcer permissionEnforcer;

    /**
     * {@link DigestSecretHasher} because {@link SecretGenerator} produced the input: 256 bits of
     * {@code SecureRandom} has no search space to enumerate, so a plain digest is enough and a
     * pepper would only add a key to rotate and lose.
     *
     * <p>Switch to {@code PepperedSecretHasher} the moment the secret is something a person chose
     * or could enumerate - a six-digit OTP has a search space of one million, so an unkeyed digest
     * of it is reversible by anyone holding the database in seconds, with no cryptographic weakness
     * involved. The two are not interchangeable and their digests are not compatible: changing
     * which one a stored column was written with silently stops every existing value from matching.
     *
     * <p>Neither is a password hasher. A password is human-chosen, reused across sites and must be
     * slow to verify - that is Argon2id, in ipie-iam-service, which owns credentials.
     */
    private final SecretHasher secretHasher = new DigestSecretHasher();

    private final SecretGenerator secretGenerator = new SecretGenerator();

    public CommonSecurityExample(CurrentUserProvider currentUserProvider, PermissionEnforcer permissionEnforcer) {
        this.currentUserProvider = currentUserProvider;
        this.permissionEnforcer = permissionEnforcer;
    }

    /**
     * The declarative gate - the default. {@code PermissionCheckAspect} runs it before the method
     * body, so there is no check to forget and no early-return path that skips it. Being AOP advice,
     * it applies only through the Spring proxy: a self-invocation is unguarded.
     */
    @RequiresPermission(UserPermissions.USER_READ)
    public String readSomethingGuarded() {
        return "visible only to a caller holding USER_READ";
    }

    /**
     * The programmatic gate, for a check that depends on a value only known inside the method - a
     * branch that needs write permission only when the request actually changes something.
     *
     * <p>Preferred over {@code @PreAuthorize} throughout this platform because it also honours the
     * {@code ipie.security.enabled=false} local-dev escape hatch. A permit-all filter chain alone
     * does not stop {@code @PreAuthorize} from denying every call, since there is still no
     * authenticated principal for it to inspect.
     */
    public String updateSomething(boolean actuallyChangesData) {
        if (actuallyChangesData) {
            permissionEnforcer.require(UserPermissions.USER_WRITE);
        }
        return "done";
    }

    /**
     * The authority string the same permission becomes inside Spring Security, for the places that
     * genuinely need one - a SpEL {@code @PreAuthorize} expression, or a filter-chain matcher. Note
     * such an expression must be a compile-time constant, i.e.
     * {@code @PreAuthorize("hasAuthority('" + PermissionAuthorities.PREFIX + "USER_READ')")}; this
     * method is for runtime comparisons.
     */
    public String authorityFor(String permission) {
        return PermissionAuthorities.authority(permission);
    }

    /**
     * The caller, as business code should read it. {@link CurrentUserProvider#current()} is an
     * {@link Optional} on purpose - a scheduled job or a broker consumer has no authenticated user,
     * and code that assumes one there fails at the least convenient moment. Use
     * {@code currentOrThrow()} only where the absence of a caller really is a programming error.
     */
    public Optional<CurrentUser> caller() {
        return currentUserProvider.current();
    }

    /** Reads the caller's permission set directly, for a decision too fine-grained for a method-level gate. */
    public boolean callerMayDelete() {
        return currentUserProvider.current()
                .map(user -> user.hasPermission(UserPermissions.USER_DELETE))
                .orElse(false);
    }

    /**
     * Issue-and-store: the plaintext is returned to the caller once (it goes into the link or the
     * message) and only the hash is ever persisted, so reading the database yields nothing usable.
     */
    public IssuedSecret issueSetupToken() {
        String plaintext = secretGenerator.newToken();
        return new IssuedSecret(plaintext, secretHasher.hash(plaintext));
    }

    /**
     * Verification. {@link SecretHasher#matches} compares in constant time - a plain
     * {@code String.equals} on the digests leaks, through timing, how many leading characters of a
     * guess were right.
     */
    public boolean setupTokenMatches(String presentedToken, String storedHash) {
        return secretHasher.matches(presentedToken, storedHash);
    }

    /**
     * The password rule, as a request DTO applies it. Keycloak is the actual control - its realm
     * policy covers every path that can set a credential, including the account console and
     * administrative resets, none of which pass through this codebase. This check exists only so a
     * user is told about a weak password on the form rather than after submitting it, which is why
     * the two must be kept in step.
     */
    public boolean isAcceptablePassword(String candidate) {
        return candidate != null
                && candidate.length() >= PasswordPolicy.MIN_LENGTH
                && candidate.length() <= PasswordPolicy.MAX_LENGTH
                && candidate.matches(PasswordPolicy.REGEX);
    }

    /** The two halves of a freshly issued bearer secret: {@code plaintext} is shown once, {@code hash} is stored. */
    public record IssuedSecret(String plaintext, String hash) {
    }
}
