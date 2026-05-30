package com.fraudengine.api;

import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.application.TransactionIngestionService;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.TransactionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionIngestionService ingestionService;
    private final TransactionEvaluationService evaluationService;

    public TransactionController(TransactionIngestionService ingestionService,
                                 TransactionEvaluationService evaluationService) {
        this.ingestionService = ingestionService;
        this.evaluationService = evaluationService;
    }

    /**
     * Accepts a transaction for asynchronous evaluation. Returns immediately;
     * the decision is retrievable via {@link #getDecision(String)}.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody TransactionRequest request) {
        ingestionService.accept(request);
        return ResponseEntity.accepted()
                .body(Map.of("transactionId", request.transactionId(), "status", "PROCESSING"));
    }

    /**
     * Returns the recorded decision for a transaction, or 404 if it has not been
     * processed yet.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<FraudDecision> getDecision(@PathVariable String transactionId) {
        return ResponseEntity.ok(evaluationService.findDecision(transactionId));
    }
}
