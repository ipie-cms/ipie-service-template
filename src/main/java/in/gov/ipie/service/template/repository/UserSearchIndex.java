package in.gov.ipie.service.template.repository;

import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.template.domain.User;
import in.gov.ipie.service.template.domain.UserSearchCriteria;

/**
 * Domain-owned port for the User *search* read model - deliberately separate from
 * {@link UserRepository}, which owns the User *write* model (source of truth). Two
 * implementations exist (see {@code infrastructure.search.UserSearchIndexConfig}): a real
 * Elasticsearch-backed one, active when {@code spring.elasticsearch.uris} is configured, and a
 * fallback that simply delegates {@link #search} to {@link UserRepository} so the service still
 * runs - with Postgres-backed search - when no Elasticsearch cluster is available (e.g. a bare
 * {@code java -jar} run with no docker-compose stack), mirroring how {@code EventPublisher}
 * falls back to {@code LoggingEventPublisher} without a Kafka broker.
 */
public interface UserSearchIndex {

    PageResult<User> search(UserSearchCriteria criteria, PageRequest pageRequest);

    /** Keyset ("seek") variant of {@link #search}; see {@code UserRepository#searchAfter}'s Javadoc. */
    CursorPageResult<User> searchAfter(UserSearchCriteria criteria, CursorPageRequest pageRequest);

    /** Upserts the given user into the search index. No-op for the Postgres-only fallback. */
    void index(User user);
}
