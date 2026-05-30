package com.fraudengine.api;

import com.fraudengine.application.TransactionEvaluationService;
import com.fraudengine.domain.model.FraudDecision;
import com.fraudengine.domain.model.TransactionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionEvaluationService evaluationService;

    public TransactionController(TransactionEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public ResponseEntity<FraudDecision> submit(@Valid @RequestBody TransactionRequest request) {
        FraudDecision decision = evaluationService.evaluate(request);
        return ResponseEntity.ok(decision);
    }
}
