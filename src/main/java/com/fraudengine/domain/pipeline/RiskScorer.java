package com.fraudengine.domain.pipeline;

import com.fraudengine.config.FraudProperties;
import com.fraudengine.domain.model.RuleViolation;

import java.util.Collection;

/**
 * Stage C of the pipeline. A pure function mapping a set of fraud signals to a
 * weighted risk score, clamped to {@value #MAX_SCORE}. No I/O.
 */
public class RiskScorer {

    static final int MAX_SCORE = 100;

    private final FraudProperties.Weights weights;

    public RiskScorer(FraudProperties.Weights weights) {
        this.weights = weights;
    }

    /**
     * Computes the risk score for a set of violations.
     *
     * @param violations the signals raised for the transaction
     * @return the summed weight of all present signals, clamped to 0..100
     */
    public int score(Collection<RuleViolation> violations) {
        int score = 0;
        for (RuleViolation violation : violations) {
            score += weightOf(violation);
        }
        return Math.min(score, MAX_SCORE);
    }

    private int weightOf(RuleViolation violation) {
        return switch (violation) {
            case BLACKLISTED_MERCHANT -> weights.blacklistedMerchant();
            case HIGH_VELOCITY -> weights.highVelocity();
            case AMOUNT_EXCEEDED -> weights.amountExceeded();
            case HIGH_RISK_LOCATION -> weights.highRiskLocation();
        };
    }
}
