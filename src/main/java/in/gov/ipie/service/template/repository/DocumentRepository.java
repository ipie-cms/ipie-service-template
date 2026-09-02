package in.gov.ipie.service.template.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.service.template.domain.Document;

/**
 * Domain-owned port for Document metadata persistence (never the file bytes - see {@code
 * FileStorage}). Same layering reasoning as {@link UserRepository}.
 */
public interface DocumentRepository {

    Document save(Document document);

    Optional<Document> findById(UUID id);

    /** All versions of documents for a case, newest first - supports the versioning rule (section 8). */
    List<Document> findByCaseId(String caseId);
}
