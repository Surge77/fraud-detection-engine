package com.fraudengine.domain.model;

import java.util.List;

/**
 * Enriched view of a {@link TransactionRequest} after the rules and velocity
 * stages have run. Carries the raw request plus the signals gathered so the
 * scorer can compute a risk score as a pure function.
 *
 * @param request        the original inbound request
 * @param violations     rule violations detected for this transaction
 * @param velocityCount  number of transactions seen for the account in the window
 */
public record Transaction(
        TransactionRequest request,
        List<RuleViolation> violations,
        int velocityCount
) {
    public Transaction {
        violations = List.copyOf(violations);
    }
}
