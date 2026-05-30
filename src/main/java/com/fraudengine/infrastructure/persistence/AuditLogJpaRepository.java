package com.fraudengine.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {

    Optional<AuditLogEntity> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);
}
