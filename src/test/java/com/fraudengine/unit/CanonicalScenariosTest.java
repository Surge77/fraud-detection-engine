package com.fraudengine.unit;

import com.fraudengine.config.FraudProperties;
import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.model.RuleViolation;
import com.fraudengine.domain.pipeline.DecisionEngine;
import com.fraudengine.domain.pipeline.RiskScorer;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * The six canonical cases from the test plan, exercising the real scorer and
 * decision engine with production weights and threshold.
 */
class CanonicalScenariosTest {

    private static final RiskScorer SCORER =
            new RiskScorer(new FraudProperties.Weights(40, 30, 20, 10));
    private static final DecisionEngine DECISION = new DecisionEngine(75);

    private void assertScenario(Set<RuleViolation> signals, int expectedScore, Decision expectedDecision) {
        int score = SCORER.score(signals);
        assertSoftly(softly -> {
            softly.assertThat(score).as("score for %s", signals).isEqualTo(expectedScore);
            softly.assertThat(DECISION.decide(score)).as("decision for %s", signals)
                    .isEqualTo(expectedDecision);
        });
    }

    @Test
    void case1_clean() {
        assertScenario(EnumSet.noneOf(RuleViolation.class), 0, Decision.PASS);
    }

    @Test
    void case2_blacklisted_only() {
        assertScenario(EnumSet.of(RuleViolation.BLACKLISTED_MERCHANT), 40, Decision.PASS);
    }

    @Test
    void case3_blacklisted_plus_location() {
        assertScenario(EnumSet.of(
                RuleViolation.BLACKLISTED_MERCHANT,
                RuleViolation.HIGH_RISK_LOCATION), 50, Decision.PASS);
    }

    @Test
    void case4_blacklisted_location_amount() {
        assertScenario(EnumSet.of(
                RuleViolation.BLACKLISTED_MERCHANT,
                RuleViolation.HIGH_RISK_LOCATION,
                RuleViolation.AMOUNT_EXCEEDED), 70, Decision.PASS);
    }

    @Test
    void case5_blacklisted_plus_velocity() {
        assertScenario(EnumSet.of(
                RuleViolation.BLACKLISTED_MERCHANT,
                RuleViolation.HIGH_VELOCITY), 70, Decision.PASS);
    }

    @Test
    void case6_blacklisted_velocity_amount() {
        assertScenario(EnumSet.of(
                RuleViolation.BLACKLISTED_MERCHANT,
                RuleViolation.HIGH_VELOCITY,
                RuleViolation.AMOUNT_EXCEEDED), 90, Decision.BLOCK);
    }
}
