package in.gov.ipie.service.template.domain;

import java.util.UUID;

/**
 * A document's re-upload version counter (master standards doc, file-upload rules, section 8:
 * "version it rather than overwrite"). {@code supersedesId} is {@code null} for a first upload.
 */
public record DocumentVersion(int number, UUID supersedesId) {
}
