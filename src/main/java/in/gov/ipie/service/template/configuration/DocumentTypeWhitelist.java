package in.gov.ipie.service.template.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import in.gov.ipie.common.core.exception.ValidationFailedException;
import in.gov.ipie.common.filestorage.validation.AllowedFileType;

/**
 * Per-{@code docType} file-type whitelists (master standards doc, file-upload rules, section 1:
 * "Whitelist allowed extensions explicitly per use case ... never one global list"). A
 * court-order upload and an evidence-photo upload should not accept the same file types - this is
 * deliberately service-specific (not something {@code common-file-storage} defines - see {@code
 * AllowedFileType}'s Javadoc for why), so a new service copies and edits this pattern, not this
 * exact class.
 */
public final class DocumentTypeWhitelist {

    private static final Map<String, Set<AllowedFileType>> BY_DOC_TYPE = Map.of(
            "court-order", Set.of(AllowedFileType.PDF),
            "claim-form", Set.of(AllowedFileType.PDF, AllowedFileType.DOCX),
            "financial-statement", Set.of(AllowedFileType.PDF, AllowedFileType.XLSX),
            "evidence", Set.of(AllowedFileType.PDF, AllowedFileType.JPG, AllowedFileType.PNG));

    private DocumentTypeWhitelist() {
    }

    public static Set<AllowedFileType> forDocType(String docType) {
        Set<AllowedFileType> allowed = BY_DOC_TYPE.get(docType);
        if (allowed == null) {
            throw new ValidationFailedException("Unknown document type: " + docType, List.of());
        }
        return allowed;
    }
}
