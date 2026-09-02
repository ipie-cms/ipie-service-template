package in.gov.ipie.service.template.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, UUID> {

    List<DocumentJpaEntity> findByCaseIdOrderByCreatedAtDesc(String caseId);
}
