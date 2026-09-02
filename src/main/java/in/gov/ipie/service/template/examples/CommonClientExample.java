package in.gov.ipie.service.template.examples;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.client.InterServiceClient;
import in.gov.ipie.common.client.request.ServiceRequest;

/**
 * Example-only reference code for {@code common-client} - not wired into any real request flow.
 * {@code "notification-service"} below is illustrative only and is not part of this project's
 * local Docker Compose stack; calling either method for real will fail closed (a {@code
 * TransientDependencyException}/circuit-breaker-open, never a fabricated success) until {@code
 * ipie.client.services.notification-service} (or the default {@code base-url-pattern}) actually
 * resolves to a running service - the same "provisioned, not consumed by default" idea already
 * used for the standby RabbitMQ broker in {@code docker-compose.yml}.
 *
 * <p>Real business code injects {@link InterServiceClient} the same way - one generic client
 * covers every target service and HTTP method, resolved by the {@code serviceName} passed to
 * {@link ServiceRequest}; see {@code common-client}'s README for the full configuration
 * (target base-url resolution, security mode, exception model).
 */
@Component
public class CommonClientExample {

    private final InterServiceClient interServiceClient;

    public CommonClientExample(InterServiceClient interServiceClient) {
        this.interServiceClient = interServiceClient;
    }

    /** A simple {@code GET} - the response body is deserialized straight into {@code responseType}. */
    public String fetchGreeting(String targetUserId) {
        return interServiceClient.exchange(
                ServiceRequest.get("notification-service", "/api/v1/greetings/" + targetUserId).build(),
                String.class);
    }

    /**
     * A state-changing call - {@code idempotencyKey} is mandatory here at {@code ServiceRequest}
     * build time (Development_Environment_Configuration.md, Section 15, Idempotency); the caller
     * decides the key, e.g. the id of whatever triggered this notification, so a retried call
     * never creates a duplicate notification downstream.
     */
    public void notifyDocumentUploaded(String documentId, String idempotencyKey) {
        interServiceClient.execute(
                ServiceRequest.post("notification-service", "/api/v1/notifications")
                        .body("{\"documentId\":\"" + documentId + "\"}")
                        .idempotencyKey(idempotencyKey)
                        .build());
    }
}
