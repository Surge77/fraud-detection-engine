package com.fraudengine.application;

import com.fraudengine.domain.exception.FraudEngineException;
import com.fraudengine.domain.model.FraudReport;
import com.fraudengine.infrastructure.persistence.FraudReportEntity;
import com.fraudengine.infrastructure.persistence.FraudReportJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Triggers the fraud-report batch job and reads stored reports.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final JobLauncher jobLauncher;
    private final Job fraudReportJob;
    private final FraudReportJpaRepository repository;

    public ReportService(JobLauncher jobLauncher, Job fraudReportJob,
                         FraudReportJpaRepository repository) {
        this.jobLauncher = jobLauncher;
        this.fraudReportJob = fraudReportJob;
        this.repository = repository;
    }

    /**
     * Runs the report job for a given date. A unique run parameter is added so
     * the same date can be regenerated (Batch deduplicates identical parameters).
     *
     * @param date the day to aggregate
     */
    public void generateFor(LocalDate date) {
        JobParameters params = new JobParametersBuilder()
                .addString("reportDate", date.toString())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobLauncher.run(fraudReportJob, params);
        } catch (Exception e) {
            throw new FraudEngineException("Failed to run fraud report job for " + date, e);
        }
    }

    /**
     * Retrieves a stored report by date.
     *
     * @param date report date
     * @return the report if present
     */
    public Optional<FraudReport> getReport(LocalDate date) {
        return repository.findByReportDate(date).map(this::toDomain);
    }

    private FraudReport toDomain(FraudReportEntity e) {
        return new FraudReport(e.getReportDate(), e.getTotalTransactions(), e.getFlaggedCount(),
                e.getFlagRate(), e.getAvgRiskScore(), e.getTopRiskyMerchants());
    }
}
