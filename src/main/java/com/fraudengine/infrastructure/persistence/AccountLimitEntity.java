package com.fraudengine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "account_limits")
public class AccountLimitEntity {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "daily_limit", nullable = false)
    private BigDecimal dailyLimit;

    protected AccountLimitEntity() {
    }

    public AccountLimitEntity(String accountId, BigDecimal dailyLimit) {
        this.accountId = accountId;
        this.dailyLimit = dailyLimit;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }
}
