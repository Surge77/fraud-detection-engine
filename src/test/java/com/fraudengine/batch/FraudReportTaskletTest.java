package com.fraudengine.batch;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FraudReportTaskletTest {

    @Test
    void flag_rate_is_zero_when_no_transactions() {
        assertThat(FraudReportTasklet.flagRate(0, 0)).isEqualByComparingTo("0.00");
    }

    @Test
    void flag_rate_is_percentage_rounded_to_two_decimals() {
        assertThat(FraudReportTasklet.flagRate(200, 50)).isEqualByComparingTo("25.00");
        assertThat(FraudReportTasklet.flagRate(3, 1)).isEqualByComparingTo("33.33");
    }

    @Test
    void flag_rate_is_hundred_when_all_flagged() {
        assertThat(FraudReportTasklet.flagRate(10, 10)).isEqualByComparingTo("100.00");
    }

    @Test
    void flag_rate_scale_is_two() {
        BigDecimal rate = FraudReportTasklet.flagRate(7, 2);
        assertThat(rate.scale()).isEqualTo(2);
    }
}
