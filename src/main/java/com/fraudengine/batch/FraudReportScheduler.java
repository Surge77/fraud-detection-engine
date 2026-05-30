package com.fraudengine.batch;

import com.fraudengine.application.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Triggers the fraud-report job nightly at midnight for the previous day.
 */
@Component
public class FraudReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(FraudReportScheduler.class);

    private final ReportService reportService;

    public FraudReportScheduler(ReportService reportService) {
        this.reportService = reportService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void runNightly() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Triggering nightly fraud report for {}", yesterday);
        reportService.generateFor(yesterday);
    }
}
