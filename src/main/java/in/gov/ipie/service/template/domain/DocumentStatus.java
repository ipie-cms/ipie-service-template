package in.gov.ipie.service.template.domain;

/**
 * Final outcome of the quarantine-first upload pipeline (master standards doc, file-upload rules,
 * section 3). There is no "pending" state - scanning happens synchronously within the upload
 * request, so a {@code Document} row is only ever persisted once the outcome is already known.
 */
public enum DocumentStatus {
    /** Scanned clean and promoted to permanent storage - {@code storageKey} points at real bytes. */
    CLEAN,
    /** Scanned infected - the bytes were deleted, not the permanent copy; this row exists for audit purposes only. */
    INFECTED
}
