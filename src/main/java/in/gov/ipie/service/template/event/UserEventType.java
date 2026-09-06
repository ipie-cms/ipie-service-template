package in.gov.ipie.service.template.event;

/** Business event names this service publishes, and the contract version they are published at. */
public enum UserEventType {
    USER_CREATED,
    USER_UPDATED,
    USER_DEACTIVATED,
    USER_REACTIVATED;

    public static final int CONTRACT_VERSION = 1;
}
