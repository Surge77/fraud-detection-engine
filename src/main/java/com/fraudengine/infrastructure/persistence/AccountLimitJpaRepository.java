package com.fraudengine.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountLimitJpaRepository extends JpaRepository<AccountLimitEntity, String> {
}
