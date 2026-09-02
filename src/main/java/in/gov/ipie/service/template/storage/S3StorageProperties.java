package in.gov.ipie.service.template.storage;

/**
 * {@code ipie.storage.s3.*} - see {@code application.yml} for local-dev (MinIO) defaults and
 * {@code S3StorageConfig}'s Javadoc for how switching backends works.
 */
public class S3StorageProperties {

    private String endpoint;
    private String publicEndpoint;
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;
    private String bucket;
    private boolean pathStyleAccess;
    private boolean sseEnabled;
    private String sseKmsKeyId;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * The endpoint embedded in presigned URLs, as opposed to {@link #getEndpoint()} (what this
     * service itself uses to reach the store). These differ whenever the app talks to the store
     * over an internal address a client (browser/Postman) can't resolve - e.g. this service
     * reaches MinIO at the docker-network hostname {@code minio}, but a presigned URL handed back
     * to an external caller needs {@code localhost} (the host-mapped port) instead. Falls back to
     * {@link #getEndpoint()} when unset, since the two are often genuinely the same (a real AWS
     * S3 endpoint is globally resolvable either way).
     */
    public String getPublicEndpoint() {
        return publicEndpoint != null && !publicEndpoint.isBlank() ? publicEndpoint : endpoint;
    }

    public void setPublicEndpoint(String publicEndpoint) {
        this.publicEndpoint = publicEndpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    public boolean isSseEnabled() {
        return sseEnabled;
    }

    public void setSseEnabled(boolean sseEnabled) {
        this.sseEnabled = sseEnabled;
    }

    public String getSseKmsKeyId() {
        return sseKmsKeyId;
    }

    public void setSseKmsKeyId(String sseKmsKeyId) {
        this.sseKmsKeyId = sseKmsKeyId;
    }
}
