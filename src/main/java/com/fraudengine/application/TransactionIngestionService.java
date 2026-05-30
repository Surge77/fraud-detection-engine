package com.fraudengine.application;

import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.domain.ports.IncomingTransactionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Entry point for the asynchronous flow. Hands an accepted transaction to the
 * pipeline and returns immediately so the API stays fast regardless of
 * downstream processing latency.
 */
@Service
public class TransactionIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionIngestionService.class);

    private final IncomingTransactionPublisher publisher;

    public TransactionIngestionService(IncomingTransactionPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Accepts a transaction for asynchronous evaluation.
     *
     * @param request the transaction to process
     */
    public void accept(TransactionRequest request) {
        publisher.publish(request);
        log.info("Accepted transaction {} for async processing", request.transactionId());
    }
}
