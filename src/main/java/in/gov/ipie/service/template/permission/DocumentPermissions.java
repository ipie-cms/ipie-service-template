package in.gov.ipie.service.template.permission;

/**
 * Permission names for the Document API, enforced via {@code PermissionEnforcer.require(...)}.
 * Same convention as {@link UserPermissions}.
 *
 * <p><b>Known gap</b> (master standards doc's gaps list): this is a permission check only, not
 * full attribute-based access control - it does not verify the caller is actually associated with
 * {@code caseId}. A real deployment needs an OPA-based ABAC check for that (master standards doc,
 * file-upload rules, section 5); this template does not build one yet.
 */
public final class DocumentPermissions {

    public static final String DOCUMENT_READ = "DOCUMENT_READ";
    public static final String DOCUMENT_WRITE = "DOCUMENT_WRITE";

    private DocumentPermissions() {
    }
}
