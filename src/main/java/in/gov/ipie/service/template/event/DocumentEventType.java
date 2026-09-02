package in.gov.ipie.service.template.event;

/** Business event names this service publishes for the Document domain, and their contract version. */
public enum DocumentEventType {
    DOCUMENT_UPLOADED,
    DOCUMENT_UPLOAD_REJECTED;

    public static final int CONTRACT_VERSION = 1;
}
