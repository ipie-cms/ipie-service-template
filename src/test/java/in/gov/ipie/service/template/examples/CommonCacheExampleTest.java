package in.gov.ipie.service.template.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Plain unit test of {@link CommonCacheExample}'s own method bodies, run without a Spring
 * context - so {@code @Cacheable}/{@code @CachePut}/{@code @CacheEvict} are inert here (Spring's
 * caching AOP only applies to calls made through a Spring-managed proxy) and every call reaches
 * the real method body. That the underlying {@code CacheManager} actually caches through Redis
 * (or safely no-ops without it) is common-cache's own concern, already proven end to end by
 * {@code IpieCacheAutoConfigurationTest} against a real Redis instance - this test only proves
 * this class's own logic is correct.
 */
class CommonCacheExampleTest {

    private final CommonCacheExample example = new CommonCacheExample();

    @Test
    void expensiveLookup_returnsAValueDerivedFromTheGivenId() {
        UUID id = UUID.randomUUID();

        String value = example.expensiveLookup(id);

        assertThat(value).isEqualTo("value-for-" + id);
        assertThat(example.loadCount()).isEqualTo(1);
    }

    @Test
    void updateAndRefreshCache_returnsTheNewValueUnchanged() {
        String result = example.updateAndRefreshCache(UUID.randomUUID(), "replacement-value");

        assertThat(result).isEqualTo("replacement-value");
    }

    @Test
    void evict_doesNotThrowForAnUnknownId() {
        example.evict(UUID.randomUUID());
    }
}
