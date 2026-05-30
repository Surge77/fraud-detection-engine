package com.fraudengine.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated daily fraud summary produced by the nightly batch job.
 *
 * @param reportDate         day the report covers
 * @param totalTransactions  transactions evaluated that day
 * @param flaggedCount       transactions blocked that day
 * @param flagRate           blocked / total as a percentage
 * @param avgRiskScore       mean risk score across the day
 * @param topRiskyMerchants  up to five merchants with the most blocks
 */
public record FraudReport(
        LocalDate reportDate,
        int totalTransactions,
        int flaggedCount,
        BigDecimal flagRate,
        BigDecimal avgRiskScore,
        List<MerchantFlagCount> topRiskyMerchants
) {
}
