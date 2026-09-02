package in.gov.ipie.service.template.storage;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import in.gov.ipie.common.filestorage.storage.FileStorage;

/**
 * Wires the {@link FileStorage} port to {@link S3FileStorage} - one implementation, speaking only
 * the S3 API, so switching the actual backend (local MinIO, real AWS S3, or any other
 * S3-API-compatible provider) is a configuration change only, never a code change (master
 * standards doc, file-upload rules). {@code ipie.storage.s3.endpoint} is the switch: set it to
 * MinIO's address for local dev, or leave it unset in a real AWS deployment so the SDK resolves
 * the real regional S3 endpoint itself.
 *
 * <p>Credentials: if {@code ipie.storage.s3.access-key}/{@code secret-key} are configured (local
 * MinIO), those are used directly; otherwise this falls back to the AWS SDK's default credential
 * chain (IAM role, environment, etc - the correct behaviour for a real AWS deployment, where
 * static keys should not be used at all).
 */
@Configuration
public class S3StorageConfig {

    @Bean
    @ConfigurationProperties(prefix = "ipie.storage.s3")
    public S3StorageProperties s3StorageProperties() {
        return new S3StorageProperties();
    }

    @Bean
    public S3Client s3Client(S3StorageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .forcePathStyle(properties.isPathStyleAccess());
        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(S3StorageProperties properties) {
        // S3Presigner.Builder has no forcePathStyle() shortcut (unlike S3ClientBuilder) - path
        // style has to go through the same S3Configuration a real client would use, or a
        // presigned URL silently comes back virtual-hosted-style (https://bucket.host/...),
        // which doesn't resolve against MinIO at all - confirmed by an actual failed download in
        // live verification, not just inspection.
        //
        // Uses getPublicEndpoint(), not getEndpoint(): this service reaches MinIO at the
        // docker-network hostname (e.g. "minio"), but a presigned URL is handed back to an
        // external caller (Postman, a browser) that can't resolve that hostname at all - it needs
        // the host-mapped address instead. Also caught by live verification, not by inspection -
        // the first fix (path-style) alone still produced a URL nothing outside the compose
        // network could actually fetch.
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.isPathStyleAccess())
                        .build());
        if (properties.getPublicEndpoint() != null && !properties.getPublicEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getPublicEndpoint()));
        }
        return builder.build();
    }

    @Bean
    public FileStorage fileStorage(S3Client s3Client, S3Presigner s3Presigner, S3StorageProperties properties) {
        return new S3FileStorage(s3Client, s3Presigner, properties);
    }

    private AwsCredentialsProvider credentialsProvider(S3StorageProperties properties) {
        if (properties.getAccessKey() != null && !properties.getAccessKey().isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}
