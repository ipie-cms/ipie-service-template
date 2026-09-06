package in.gov.ipie.service.template.domain;

/** Where a document belongs - the business case and document type it was uploaded under. */
public record DocumentContext(String caseId, String docType) {
}
