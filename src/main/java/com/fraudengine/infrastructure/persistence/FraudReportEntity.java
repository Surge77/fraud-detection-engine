package com.fraudengine.infrastructure.persistence;

import com.fraudengine.domain.model.MerchantFlagCount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "fraud_reports")
public class FraudReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "total_transactions")
    private Integer totalTransactions;

    @Column(name = "flagged_count")
    private Integer flaggedCount;

    @Column(name = "flag_rate")
    private BigDecimal flagRate;

    @Column(name = "avg_risk_score")
    private BigDecimal avgRiskScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_risky_merchants", columnDefinition = "jsonb")
    private List<MerchantFlagCount> topRiskyMerchants;

    protected FraudReportEntity() {
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public Integer getTotalTransactions() {
        return totalTransactions;
    }

    public Integer getFlaggedCount() {
        return flaggedCount;
    }

    public BigDecimal getFlagRate() {
        return flagRate;
    }

    public BigDecimal getAvgRiskScore() {
        return avgRiskScore;
    }

    public List<MerchantFlagCount> getTopRiskyMerchants() {
        return topRiskyMerchants;
    }
}
