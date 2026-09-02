package in.gov.ipie.service.template.domain;

/**
 * What the uploaded file actually is: the original filename (kept as metadata only, never as the
 * storage path - master standards doc, file-upload rules, section 4), the {@code FileStorage} key
 * ({@code null} when {@link DocumentStatus#INFECTED} - the bytes were never durably stored),
 * sniffed content type, size and SHA-256 hash.
 */
public record DocumentFile(String originalFilename, String storageKey, String contentType, long sizeBytes, String sha256Hash) {
}
