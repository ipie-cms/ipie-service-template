package in.gov.ipie.service.template.examples;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.persistence.AuditableJpaEntity;
import in.gov.ipie.common.persistence.IdCollisionException;
import in.gov.ipie.common.persistence.IntegrityViolations;

/**
 * Example-only reference code for {@code common-persistence} - not wired into any real request
 * flow. {@code UserRepositoryImpl} and {@code IntegrityViolationsConfig} do this for real; the
 * declaration below is the shape a new table's repository copies.
 *
 * <p><b>What the rest of this package does without being called.</b> {@link AuditableJpaEntity} is
 * the mapped superclass every JPA entity extends for the eight audit/soft-delete columns
 * (master standards doc, 7.2) - it already carries {@code @EntityListeners}, so a subclass must not
 * repeat it. {@code JpaAuditingAutoConfiguration} fills {@code createdBy}/{@code updatedBy} from the
 * authenticated caller, and {@code IpieDataSourceAutoConfiguration} plus
 * {@code ReadWriteRoutingDataSourceAutoConfiguration} (read-replica routing by
 * {@code @Transactional(readOnly = true)}) are pure configuration. None of them has a caller-facing
 * API, which leaves {@link IntegrityViolations} as the one class a service actually writes code
 * against.
 *
 * <p><b>Why it exists.</b> Repositories used to catch {@link DataIntegrityViolationException} and
 * report one fixed message - in ipie-user-service, "a user with the same username or email already
 * exists". That catch is unconditional, so it also answered for a duplicate phone number, a broken
 * foreign key and a primary-key collision. A caller whose phone number was taken was told their
 * email was, and whoever debugged it started in the wrong place.
 *
 * <p><b>And why retrying is the wrong instinct.</b> Retrying a duplicate-email insert turns one
 * correct 409 into several pointless attempts and then a 500. Only an id collision is fixable by
 * trying again - which is exactly why the constraint name has to be read before anything is
 * decided. Even then this reports {@link IdCollisionException} rather than retrying: Hibernate has
 * already assigned the generated id to the instance, so saving it again collides identically, and
 * only the caller can mint a fresh one.
 */
@Component
public class CommonPersistenceExample {

    /**
     * One declaration per table, because only the repository that owns the table knows its
     * constraint names. Each message names the field the caller actually repeated - that is the
     * entire point, and a generic "duplicate value" message gives it all back.
     *
     * <p>{@code otherwise} is deliberately omitted here. Without it, a constraint this declaration
     * does not know - a foreign key, a check, one added by a later migration - is rethrown
     * unchanged and surfaces as a 500 with the real cause intact. Absorbing it into a generic 409
     * would be a guess, and would hide a bug that is worth seeing.
     */
    private static final IntegrityViolations VIOLATIONS = IntegrityViolations.forTable()
            .primaryKey("example_records_pkey")
            .conflict("uq_example_records_reference", "An example record with this reference already exists")
            .conflict("uq_example_records_external_id", "An example record with this external id already exists")
            .build();

    /**
     * The call site. {@code translate} always returns rather than throws, so the statement reads
     * {@code throw VIOLATIONS.translate(e)} and the compiler still sees the method end.
     */
    public String save(String reference, boolean simulateDatabaseRejection) {
        try {
            if (simulateDatabaseRejection) {
                throw new DataIntegrityViolationException("simulated constraint violation");
            }
            return reference;
        } catch (DataIntegrityViolationException e) {
            throw VIOLATIONS.translate(e);
        }
    }

    /**
     * The same declaration has to be reachable as a {@code @Bean} as well - see
     * {@code IntegrityViolationsConfig}. A repository's own {@code catch} only fires when Hibernate
     * flushes inside the repository call; with the insert deferred to commit, the violation
     * surfaces after the repository has returned and {@code GlobalExceptionHandler} is the only
     * thing left on the stack. A table registered in the repository but not published as a bean is
     * a 409 that arrives as a 500.
     */
    public IntegrityViolations declarationForTheErrorBoundary() {
        return VIOLATIONS;
    }

    /** The constraint the database actually rejected on, when it named one - empty for a nullability or type failure. */
    public Optional<String> constraintThatFailed(Throwable failure) {
        return IntegrityViolations.constraintName(failure);
    }
}
