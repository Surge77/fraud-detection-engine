package com.fraudengine.unit;

import com.fraudengine.domain.model.VelocityResult;
import com.fraudengine.domain.pipeline.VelocityChecker;
import com.fraudengine.domain.ports.VelocityPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VelocityCheckerTest {

    private static final int WINDOW_SECONDS = 300;
    private static final int MAX_TRANSACTIONS = 10;

    @Mock
    private VelocityPort velocityPort;

    private VelocityChecker checker() {
        return new VelocityChecker(velocityPort, WINDOW_SECONDS, MAX_TRANSACTIONS);
    }

    @Test
    void count_below_max_is_not_exceeded() {
        when(velocityPort.incrementAndCount("acc_001", WINDOW_SECONDS)).thenReturn(5L);
        VelocityResult result = checker().record("acc_001");
        assertThat(result.count()).isEqualTo(5);
        assertThat(result.exceeded()).isFalse();
    }

    @Test
    void count_at_max_is_not_exceeded() {
        when(velocityPort.incrementAndCount("acc_001", WINDOW_SECONDS)).thenReturn(10L);
        assertThat(checker().record("acc_001").exceeded()).isFalse();
    }

    @Test
    void count_above_max_is_exceeded() {
        when(velocityPort.incrementAndCount("acc_001", WINDOW_SECONDS)).thenReturn(11L);
        assertThat(checker().record("acc_001").exceeded()).isTrue();
    }

    @Test
    void passes_configured_window_to_port() {
        when(velocityPort.incrementAndCount("acc_001", WINDOW_SECONDS)).thenReturn(1L);
        checker().record("acc_001");
        verify(velocityPort).incrementAndCount("acc_001", WINDOW_SECONDS);
    }

    @Test
    void fails_open_when_velocity_store_unavailable() {
        when(velocityPort.incrementAndCount("acc_001", WINDOW_SECONDS))
                .thenThrow(new RuntimeException("redis down"));

        var result = checker().record("acc_001");

        assertThat(result.count()).isZero();
        assertThat(result.exceeded()).isFalse();
    }
}
