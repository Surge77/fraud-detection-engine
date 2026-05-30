package com.fraudengine.api;

import com.fraudengine.application.ReportService;
import com.fraudengine.domain.exception.TransactionNotFoundException;
import com.fraudengine.domain.model.FraudReport;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Runs the report job immediately. Defaults to yesterday when no date is given.
     */
    @PostMapping("/admin/reports/trigger")
    public ResponseEntity<Map<String, String>> trigger(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now().minusDays(1);
        reportService.generateFor(target);
        return ResponseEntity.accepted().body(Map.of("status", "TRIGGERED", "date", target.toString()));
    }

    /**
     * Returns the stored report for a date, or 404 if none exists.
     */
    @GetMapping("/reports")
    public ResponseEntity<FraudReport> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportService.getReport(date)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new TransactionNotFoundException("report for " + date));
    }
}
