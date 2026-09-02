package in.gov.ipie.service.template.examples;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Example-only reference code for {@code common-cache} - not wired into any real request flow.
 * Real business code just adds these same annotations directly to a real application-service
 * method (e.g. {@code UserService.getUser}) - no extra wiring needed, since {@code
 * IpieCacheAutoConfiguration} already provides a {@link org.springframework.cache.CacheManager}
 * (Redis-backed once {@code spring.data.redis.host} is configured, a no-op fallback otherwise, so
 * these annotations are always safe to add).
 *
 * <p>Cache name is {@code example-lookup}, deliberately distinct from any real cache name a
 * service defines (e.g. {@code user-lookup}, referenced in {@code application.yml}'s {@code
 * ipie.cache.ttls} comment), so this example never collides with or shares TTL configuration with
 * a real cache.
 */
@Component
public class CommonCacheExample {

    private final AtomicInteger loadCount = new AtomicInteger();

    /**
     * {@code @Cacheable} - the method body only runs on a cache miss; a repeated call with the
     * same {@code id} returns the cached value without re-executing this method (see {@link
     * #loadCount()}, used by this class's test to prove that).
     */
    @Cacheable(cacheNames = "example-lookup", key = "#id")
    public String expensiveLookup(UUID id) {
        loadCount.incrementAndGet();
        return "value-for-" + id;
    }

    /**
     * {@code @CachePut} - unlike {@code @Cacheable}, the method body always runs (this is the
     * write path after an update), and its return value replaces whatever was cached under
     * {@code id} - the next {@link #expensiveLookup(UUID)} for the same id returns this value
     * without hitting the underlying store again.
     */
    @CachePut(cacheNames = "example-lookup", key = "#id")
    public String updateAndRefreshCache(UUID id, String newValue) {
        return newValue;
    }

    /** {@code @CacheEvict} - removes a stale entry, e.g. after a delete, forcing the next lookup to be a real miss. */
    @CacheEvict(cacheNames = "example-lookup", key = "#id")
    public void evict(UUID id) {
    }

    /** How many times {@link #expensiveLookup(UUID)}'s body actually ran (i.e. cache misses), for tests only. */
    public int loadCount() {
        return loadCount.get();
    }
}
