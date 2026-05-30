package com.fraudengine.domain.pipeline;

import com.fraudengine.domain.model.Decision;

/**
 * Final stage. Maps a risk score to a terminal decision using the configured
 * threshold: a score at or above the threshold blocks the transaction.
 */
public class DecisionEngine {

    private final int threshold;

    public DecisionEngine(int threshold) {
        this.threshold = threshold;
    }

    /**
     * @param score risk score, 0..100
     * @return BLOCK if {@code score >= threshold}, otherwise PASS
     */
    public Decision decide(int score) {
        return score >= threshold ? Decision.BLOCK : Decision.PASS;
    }
}
