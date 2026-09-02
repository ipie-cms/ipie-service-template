package in.gov.ipie.service.template.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import in.gov.ipie.common.client.DefaultInterServiceClient;
import in.gov.ipie.common.client.config.InterServiceClientProperties;
import in.gov.ipie.common.client.config.ResilienceRegistries;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;

/**
 * Proves {@link CommonClientExample} actually round-trips through {@link
 * in.gov.ipie.common.client.InterServiceClient} against a real (stubbed) HTTP server - built
 * directly, the same way common-client's own {@code DefaultInterServiceClientTest} does, rather
 * than through the full Spring Boot autoconfiguration: this avoids needing this service's own
 * database/Flyway/Elasticsearch just to prove one example class's wiring is correct.
 */
class CommonClientExampleTest {

    private HttpServer server;
    private CommonClientExample example;
    private volatile String lastRequestBody;
    private volatile String lastIdempotencyKeyHeader;

    @BeforeEach
    void startStubNotificationService() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v1/greetings/user-42", exchange -> respond(exchange, 200, "Hello, user-42"));
        server.createContext("/api/v1/notifications", exchange -> {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastIdempotencyKeyHeader = exchange.getRequestHeaders().getFirst("Idempotency-Key");
            respond(exchange, 201, "{\"status\":\"queued\"}");
        });
        server.start();

        InterServiceClientProperties properties = new InterServiceClientProperties();
        properties.setServices(Map.of("notification-service", "http://localhost:" + server.getAddress().getPort()));

        ResilienceRegistries resilienceRegistries = new ResilienceRegistries(
                CircuitBreakerRegistry.ofDefaults(), RetryRegistry.ofDefaults(), BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults());
        DefaultInterServiceClient interServiceClient = new DefaultInterServiceClient(
                RestClient.builder(), properties, resilienceRegistries, event -> { }, "ipie-service-template");
        example = new CommonClientExample(interServiceClient);
    }

    @AfterEach
    void stopStubNotificationService() {
        server.stop(0);
    }

    @Test
    void fetchGreeting_returnsTheStubbedNotificationServicesResponseBody() {
        assertThat(example.fetchGreeting("user-42")).isEqualTo("Hello, user-42");
    }

    @Test
    void notifyDocumentUploaded_sendsTheDocumentIdAndIdempotencyKey() {
        example.notifyDocumentUploaded("doc-123", "idem-key-1");

        assertThat(lastRequestBody).isEqualTo("{\"documentId\":\"doc-123\"}");
        assertThat(lastIdempotencyKeyHeader).isEqualTo("idem-key-1");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }
}
