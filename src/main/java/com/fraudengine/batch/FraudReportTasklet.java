package com.fraudengine.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudengine.domain.exception.FraudEngineException;
import com.fraudengine.domain.model.MerchantFlagCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Aggregates the previous day's {@code audit_log} rows into a single
 * {@code fraud_reports} row. Implemented as a Tasklet because the output is one
 * reduced row, not a per-item transformation. The upsert keys on
 * {@code report_date}, so re-running for a date is idempotent.
 */
public class FraudReportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(FraudReportTasklet.class);
    static final String REPORT_DATE_PARAM = "reportDate";
    private static final int TOP_MERCHANTS = 5;
    private static final int RATE_SCALE = 2;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public FraudReportTasklet(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate date = LocalDate.parse(
                chunkContext.getStepContext().getJobParameters().get(REPORT_DATE_PARAM).toString());

        int total = count("SELECT count(*) FROM audit_log WHERE created_at::date = ?", date);
        int flagged = count(
                "SELECT count(*) FROM audit_log WHERE created_at::date = ? AND decision = 'BLOCK'", date);
        BigDecimal avgScore = jdbc.queryForObject(
                "SELECT COALESCE(AVG(risk_score), 0) FROM audit_log WHERE created_at::date = ?",
                BigDecimal.class, date);
        BigDecimal flagRate = flagRate(total, flagged);
        List<MerchantFlagCount> topMerchants = topRiskyMerchants(date);

        jdbc.update("""
                INSERT INTO fraud_reports
                    (report_date, total_transactions, flagged_count, flag_rate, avg_risk_score, top_risky_merchants, created_at)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, now())
                ON CONFLICT (report_date) DO UPDATE SET
                    total_transactions = EXCLUDED.total_transactions,
                    flagged_count = EXCLUDED.flagged_count,
                    flag_rate = EXCLUDED.flag_rate,
                    avg_risk_score = EXCLUDED.avg_risk_score,
                    top_risky_merchants = EXCLUDED.top_risky_merchants,
                    created_at = now()
                """, date, total, flagged, flagRate,
                avgScore == null ? BigDecimal.ZERO : avgScore.setScale(RATE_SCALE, RoundingMode.HALF_UP),
                toJson(topMerchants));

        log.info("Fraud report for {}: total={}, flagged={}, rate={}%", date, total, flagged, flagRate);
        return RepeatStatus.FINISHED;
    }

    /**
     * Computes the flag rate as a percentage rounded to two decimals.
     *
     * @param total   total transactions
     * @param flagged blocked transactions
     * @return flagged/total * 100, or zero when there are no transactions
     */
    static BigDecimal flagRate(int total, int flagged) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(flagged)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private int count(String sql, LocalDate date) {
        Integer value = jdbc.queryForObject(sql, Integer.class, date);
        return value == null ? 0 : value;
    }

    private List<MerchantFlagCount> topRiskyMerchants(LocalDate date) {
        return jdbc.query("""
                SELECT merchant_id, count(*) AS flag_count
                FROM audit_log
                WHERE created_at::date = ? AND decision = 'BLOCK' AND merchant_id IS NOT NULL
                GROUP BY merchant_id
                ORDER BY flag_count DESC
                LIMIT ?
                """, (rs, rowNum) ->
                new MerchantFlagCount(rs.getString("merchant_id"), rs.getLong("flag_count")), date, TOP_MERCHANTS);
    }

    private String toJson(List<MerchantFlagCount> merchants) {
        try {
            return objectMapper.writeValueAsString(merchants);
        } catch (JsonProcessingException e) {
            throw new FraudEngineException("Failed to serialize top merchants", e);
        }
    }
}
