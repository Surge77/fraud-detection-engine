package com.fraudengine.unit;

import com.fraudengine.domain.model.Decision;
import com.fraudengine.domain.pipeline.DecisionEngine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionEngineTest {

    private static final int THRESHOLD = 75;
    private final DecisionEngine engine = new DecisionEngine(THRESHOLD);

    @Test
    void score_just_below_threshold_passes() {
        assertThat(engine.decide(74)).isEqualTo(Decision.PASS);
    }

    @Test
    void score_exactly_at_threshold_blocks() {
        assertThat(engine.decide(75)).isEqualTo(Decision.BLOCK);
    }

    @Test
    void score_above_threshold_blocks() {
        assertThat(engine.decide(76)).isEqualTo(Decision.BLOCK);
    }

    @Test
    void zero_score_passes() {
        assertThat(engine.decide(0)).isEqualTo(Decision.PASS);
    }
}
