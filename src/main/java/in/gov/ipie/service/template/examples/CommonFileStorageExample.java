package in.gov.ipie.service.template.examples;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Set;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.filestorage.exception.MalwareDetectedException;
import in.gov.ipie.common.filestorage.exception.ScanUnavailableException;
import in.gov.ipie.common.filestorage.hash.FileHasher;
import in.gov.ipie.common.filestorage.naming.StorageKeyGenerator;
import in.gov.ipie.common.filestorage.scanning.ScanResult;
import in.gov.ipie.common.filestorage.scanning.ScanStatus;
import in.gov.ipie.common.filestorage.scanning.VirusScanner;
import in.gov.ipie.common.filestorage.storage.FileStorage;
import in.gov.ipie.common.filestorage.validation.AllowedFileType;
import in.gov.ipie.common.filestorage.validation.FileSizeValidator;
import in.gov.ipie.common.filestorage.validation.FileTypeValidator;

/**
 * Example-only reference code for {@code common-file-storage} - not wired into any real request
 * flow. {@code DocumentServiceImpl} runs this same pipeline for real against multipart uploads;
 * this class is the same sequence reduced to a byte array, so the <em>order</em> of the steps is
 * readable without the surrounding document domain.
 *
 * <p><b>The order is the standard.</b> Validate, hash, write to quarantine, scan, and only then
 * promote (master standards doc, file-upload rules, section 3). Scanning after promotion means
 * malware has already been reachable at its permanent key; skipping quarantine entirely means it
 * always was.
 *
 * <p><b>Three things here are not what a first attempt would write.</b> The whitelist is per use
 * case, never global - a case-order upload and a profile photo have no business sharing one list.
 * The type check reads the file's magic bytes, never its extension or its {@code Content-Type}
 * header, both of which the client chooses. And a scanner that could not answer is not a clean
 * file: {@link ScanStatus#ERROR} rejects the upload, which is why {@code FailClosedVirusScanner}
 * is this template's default when no scanner is configured at all.
 */
@Component
public class CommonFileStorageExample {

    /** Per-use-case whitelist - deliberately declared at the call site's own use case, not platform-wide. */
    private static final Set<AllowedFileType> ALLOWED_TYPES = Set.of(AllowedFileType.PDF, AllowedFileType.PNG);

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    /**
     * Stateless and thread-safe, so one instance is enough - the same {@code static final} treatment
     * {@code DocumentServiceImpl} gives it. It exists so Tika stays a dependency of the platform
     * rather than of every service that validates an upload.
     */
    private static final FileTypeValidator FILE_TYPE_VALIDATOR = new FileTypeValidator();

    private final FileStorage fileStorage;
    private final VirusScanner virusScanner;

    public CommonFileStorageExample(FileStorage fileStorage, VirusScanner virusScanner) {
        this.fileStorage = fileStorage;
        this.virusScanner = virusScanner;
    }

    /**
     * The whole quarantine-first pipeline. Both keys come from one
     * {@link StorageKeyGenerator#newObjectPath} call, which is what makes promotion a same-suffix
     * copy between two prefixes rather than a second, independently-generated name.
     *
     * <p>The original filename never reaches the key - it is caller-supplied text, and a storage
     * path built from it is both a traversal risk and unpredictable to apply lifecycle rules to.
     * Keep it as metadata on the row instead, which is what {@link StoredFile} carries.
     */
    public StoredFile storeAttachment(String caseId, byte[] content, String extension) {
        FileSizeValidator.validate(content.length, MAX_BYTES);
        FILE_TYPE_VALIDATOR.validate(content, ALLOWED_TYPES);
        String contentType = FILE_TYPE_VALIDATOR.detectMimeType(content);
        String sha256Hex = FileHasher.sha256Hex(content);

        String objectPath = StorageKeyGenerator.newObjectPath("example", caseId, "attachment", extension);
        String quarantineKey = StorageKeyGenerator.quarantineKey(objectPath);
        fileStorage.put(quarantineKey, new ByteArrayInputStream(content), content.length, contentType);

        ScanResult scanResult = virusScanner.scan(new ByteArrayInputStream(content), content.length);
        if (scanResult.status() == ScanStatus.ERROR) {
            fileStorage.delete(quarantineKey);
            throw new ScanUnavailableException();
        }
        if (scanResult.status() == ScanStatus.INFECTED) {
            // The bytes go, the audit record stays - see CommonAuditExample.recordRefusal for why a
            // rejection needs an explicit AuditEvent rather than @Auditable.
            fileStorage.delete(quarantineKey);
            throw new MalwareDetectedException();
        }

        String permanentKey = StorageKeyGenerator.permanentKey(objectPath);
        fileStorage.copy(quarantineKey, permanentKey);
        fileStorage.delete(quarantineKey);
        return new StoredFile(permanentKey, sha256Hex, contentType, content.length);
    }

    /**
     * Downloads go through a short-lived signed URL handed back to the caller, not through this
     * service streaming the bytes (master standards doc, file-upload rules, section 5). Proxying
     * puts every download on the service's own heap and connection pool for no benefit.
     *
     * <p>The expiry is the access control here, so keep it short - the URL needs no token of ours
     * and works for anyone holding it until it lapses.
     */
    public String downloadUrlFor(String storageKey) {
        return fileStorage.presignedDownloadUrl(storageKey, Duration.ofMinutes(5));
    }

    /**
     * What a service records on its own metadata row. {@code sha256Hex} is the dedup/integrity
     * value (file-upload rules, section 6) and {@code contentType} is the <em>sniffed</em> type,
     * not the one the client claimed.
     */
    public record StoredFile(String storageKey, String sha256Hex, String contentType, long sizeBytes) {
    }
}
