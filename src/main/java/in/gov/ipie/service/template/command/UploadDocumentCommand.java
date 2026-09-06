package in.gov.ipie.service.template.command;

import java.util.UUID;

/**
 * {@code content} is the whole file's bytes, read once by the controller from the multipart
 * upload - kept as a single array (not a stream) so the same bytes can be validated, hashed,
 * scanned and stored without re-reading, appropriate given the per-file size limits this use case
 * assumes (master standards doc, file-upload rules, section 2). Defensively copied in and out
 * (unlike a typical record accessor) since, unlike this record's other fields, a {@code byte[]} is
 * mutable - a caller retaining a reference must not be able to alter the file content underneath
 * {@code DocumentService} mid-pipeline.
 */
public record UploadDocumentCommand(
        String caseId,
        String docType,
        String originalFilename,
        byte[] content,
        UUID supersedesId) {

    public UploadDocumentCommand {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
