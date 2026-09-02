package in.gov.ipie.service.template.storage;

import java.io.InputStream;
import java.time.Duration;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import in.gov.ipie.common.filestorage.storage.FileStorage;

/**
 * S3-API {@link FileStorage} binding - works unchanged against AWS S3, MinIO, or any other
 * S3-API-compatible target; only {@link S3StorageConfig}'s wiring (endpoint/credentials) changes
 * between them. Encrypts at rest via {@code ipie.storage.s3.sse.*} (master standards doc,
 * file-upload rules, section 4) - SSE-KMS when a real KMS key id is configured, SSE-S3 (AES256)
 * otherwise when encryption is enabled but no KMS key is available (e.g. MinIO in local dev).
 */
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    public S3FileStorage(S3Client s3Client, S3Presigner s3Presigner, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public void put(String key, InputStream content, long sizeBytes, String contentType) {
        PutObjectRequest.Builder request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType);
        applyServerSideEncryption(request);
        s3Client.putObject(request.build(), RequestBody.fromInputStream(content, sizeBytes));
    }

    @Override
    public void copy(String sourceKey, String destinationKey) {
        CopyObjectRequest.Builder request = CopyObjectRequest.builder()
                .sourceBucket(properties.getBucket())
                .sourceKey(sourceKey)
                .destinationBucket(properties.getBucket())
                .destinationKey(destinationKey);
        applyServerSideEncryption(request);
        s3Client.copyObject(request.build());
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
    }

    @Override
    public String presignedDownloadUrl(String key, Duration expiry) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(getObjectRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private void applyServerSideEncryption(PutObjectRequest.Builder request) {
        if (!properties.isSseEnabled()) {
            return;
        }
        if (properties.getSseKmsKeyId() != null && !properties.getSseKmsKeyId().isBlank()) {
            request.serverSideEncryption(ServerSideEncryption.AWS_KMS).ssekmsKeyId(properties.getSseKmsKeyId());
        } else {
            request.serverSideEncryption(ServerSideEncryption.AES256);
        }
    }

    private void applyServerSideEncryption(CopyObjectRequest.Builder request) {
        if (!properties.isSseEnabled()) {
            return;
        }
        if (properties.getSseKmsKeyId() != null && !properties.getSseKmsKeyId().isBlank()) {
            request.serverSideEncryption(ServerSideEncryption.AWS_KMS).ssekmsKeyId(properties.getSseKmsKeyId());
        } else {
            request.serverSideEncryption(ServerSideEncryption.AES256);
        }
    }
}
