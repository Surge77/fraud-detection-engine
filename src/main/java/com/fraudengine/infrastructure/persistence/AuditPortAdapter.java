package com.fraudengine.infrastructure.persistence;

import com.fraudengine.domain.model.AuditRecord;
import com.fraudengine.domain.ports.AuditPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA-backed implementation of {@link AuditPort}. Translates between the domain
 * {@link AuditRecord} and the {@link AuditLogEntity} persistence model so the
 * domain never depends on JPA.
 */
@Component
public class AuditPortAdapter implements AuditPort {

    private final AuditLogJpaRepository repository;

    public AuditPortAdapter(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AuditRecord record) {
        repository.save(toEntity(record));
    }

    @Override
    public Optional<AuditRecord> findByTransactionId(String transactionId) {
        return repository.findByTransactionId(transactionId).map(this::toDomain);
    }

    @Override
    public boolean existsByTransactionId(String transactionId) {
        return repository.existsByTransactionId(transactionId);
    }

    private AuditLogEntity toEntity(AuditRecord r) {
        return new AuditLogEntity(
                r.transactionId(), r.accountId(), r.merchantId(), r.amount(),
                r.currency(), r.location(), r.riskScore(), r.decision(),
                r.reasons(), r.createdAt());
    }

    private AuditRecord toDomain(AuditLogEntity e) {
        return new AuditRecord(
                e.getTransactionId(), e.getAccountId(), e.getMerchantId(), e.getAmount(),
                e.getCurrency(), e.getLocation(),
                e.getRiskScore() == null ? 0 : e.getRiskScore(),
                e.getDecision(), e.getReasons(), e.getCreatedAt());
    }
}
