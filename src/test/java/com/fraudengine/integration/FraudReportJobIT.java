package com.fraudengine.integration;

import com.fraudengine.application.ReportService;
import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.FraudReport;
import com.fraudengine.domain.model.TransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Spring Batch report job end-to-end: it runs against the
 * Flyway-provisioned BATCH_* metadata tables (proving the V2 migration replaces
 * Boot's initialize-schema), aggregates today's audit rows, and upserts a
 * fraud_reports row.
 */
class FraudReportJobIT extends AbstractIntegrationTest {

    @Autowired
    private TransactionEvaluationService evaluationService;

    @Autowired
    private ReportService reportService;

    private TransactionRequest tx() {
        return new TransactionRequest(
                UUID.randomUUID().toString(), "acc_003", new BigDecimal("100.00"), "USD",
                "merch_999", "Clean Store", "United States", Instant.now());
    }

    @Test
    void report_job_aggregates_todays_transactions() {
        evaluationService.evaluate(tx());
        evaluationService.evaluate(tx());

        LocalDate today = LocalDate.now();
        reportService.generateFor(today);

        Optional<FraudReport> report = reportService.getReport(today);
        assertThat(report).isPresent();
        assertThat(report.get().totalTransactions()).isGreaterThanOrEqualTo(2);
        assertThat(report.get().flagRate()).isNotNull();
    }
}
