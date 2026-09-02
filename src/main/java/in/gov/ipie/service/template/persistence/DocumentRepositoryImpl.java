package in.gov.ipie.service.template.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import in.gov.ipie.service.template.domain.Document;
import in.gov.ipie.service.template.repository.DocumentRepository;

/**
 * Infrastructure-layer adapter implementing the domain-owned {@link DocumentRepository} port.
 * {@link #save} is always an insert - document rows are immutable once created (master standards
 * doc, file-upload rules, section 8: re-uploads are new versions, never overwrites), so there is
 * no find-then-update branch here unlike {@code UserRepositoryImpl}.
 */
@Repository
class DocumentRepositoryImpl implements DocumentRepository {

    private final DocumentJpaRepository jpaRepository;
    private final DocumentPersistenceMapper mapper;

    DocumentRepositoryImpl(DocumentJpaRepository jpaRepository, DocumentPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Document save(Document document) {
        return mapper.toDomain(jpaRepository.save(mapper.toNewEntity(document)));
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Document> findByCaseId(String caseId) {
        return jpaRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream().map(mapper::toDomain).toList();
    }
}
