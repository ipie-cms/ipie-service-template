package in.gov.ipie.service.template.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import in.gov.ipie.service.template.repository.UserRepository;
import in.gov.ipie.service.template.repository.UserSearchIndex;

/**
 * Chooses the {@link UserSearchIndex} implementation: {@link ElasticsearchUserSearchIndex} when
 * {@code spring.elasticsearch.uris} is configured (e.g. {@code IPIE_ELASTICSEARCH_URIS} in
 * docker-compose.yml), {@link JpaUserSearchIndex} otherwise - mirrors
 * {@code EventPublisherConfig}'s Kafka/logging fallback pattern.
 */
@Configuration
public class UserSearchIndexConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.elasticsearch", name = "uris")
    public UserSearchIndex elasticsearchUserSearchIndex(
            ElasticsearchOperations operations, UserDocumentRepository documentRepository, UserSearchDocumentMapper mapper) {
        IndexOperations indexOps = operations.indexOps(UserDocument.class);
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping();
        }
        return new ElasticsearchUserSearchIndex(operations, documentRepository, mapper);
    }

    @Bean
    @ConditionalOnMissingBean(UserSearchIndex.class)
    public UserSearchIndex jpaUserSearchIndex(UserRepository userRepository) {
        return new JpaUserSearchIndex(userRepository);
    }
}
