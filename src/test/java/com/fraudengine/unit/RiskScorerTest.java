package com.fraudengine.unit;

import com.fraudengine.config.FraudProperties;
import com.fraudengine.domain.model.RuleViolation;
import com.fraudengine.domain.pipeline.RiskScorer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScorerTest {

    private static final FraudProperties.Weights WEIGHTS =
            new FraudProperties.Weights(40, 30, 20, 10);

    private final RiskScorer scorer = new RiskScorer(WEIGHTS);

    @Test
    void no_signals_scores_zero() {
        assertThat(scorer.score(List.of())).isZero();
    }

    @Test
    void all_signals_score_one_hundred() {
        assertThat(scorer.score(EnumSet.allOf(RuleViolation.class))).isEqualTo(100);
    }

    @Test
    void individual_weights_are_applied() {
        assertThat(scorer.score(Set.of(RuleViolation.BLACKLISTED_MERCHANT))).isEqualTo(40);
        assertThat(scorer.score(Set.of(RuleViolation.HIGH_VELOCITY))).isEqualTo(30);
        assertThat(scorer.score(Set.of(RuleViolation.AMOUNT_EXCEEDED))).isEqualTo(20);
        assertThat(scorer.score(Set.of(RuleViolation.HIGH_RISK_LOCATION))).isEqualTo(10);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allSixteenCombinations")
    void scores_every_combination_as_the_weighted_sum(Set<RuleViolation> signals, int expected) {
        assertThat(scorer.score(signals)).isEqualTo(expected);
    }

    static Stream<Arguments> allSixteenCombinations() {
        RuleViolation[] all = RuleViolation.values();
        Stream.Builder<Arguments> builder = Stream.builder();
        for (int mask = 0; mask < (1 << all.length); mask++) {
            EnumSet<RuleViolation> signals = EnumSet.noneOf(RuleViolation.class);
            int expected = 0;
            for (int i = 0; i < all.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    signals.add(all[i]);
                    expected += weightOf(all[i]);
                }
            }
            builder.add(Arguments.of(signals, Math.min(expected, 100)));
        }
        return builder.build();
    }

    private static int weightOf(RuleViolation v) {
        return switch (v) {
            case BLACKLISTED_MERCHANT -> 40;
            case HIGH_VELOCITY -> 30;
            case AMOUNT_EXCEEDED -> 20;
            case HIGH_RISK_LOCATION -> 10;
        };
    }
}
