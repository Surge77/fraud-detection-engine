package com.fraudengine.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fraudengine.domain.model.TransactionRequest;
import com.fraudengine.infrastructure.kafka.ledger.LedgerTransferEvent;
import com.fraudengine.infrastructure.kafka.ledger.LedgerTransferMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The anti-corruption layer is where the two services' domains disagree, so these
 * cases pin the translation rules rather than the plumbing.
 */
class LedgerTransferMapperTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-07-26T10:15:30Z");

    private static LedgerTransferEvent event(long amountMinor, String currency) {
        return new LedgerTransferEvent(42L, 1001L, 2002L, amountMinor, currency);
    }

    @Test
    @DisplayName("maps ledger identifiers and amount onto a scorable request")
    void toTransactionRequest_mapsCoreFields() {
        TransactionRequest request =
                LedgerTransferMapper.toTransactionRequest(event(12_345L, "USD"), RECEIVED_AT);

        assertThat(request.transactionId()).isEqualTo("42");
        assertThat(request.accountId()).isEqualTo("1001");
        assertThat(request.amount()).isEqualByComparingTo(new BigDecimal("123.45"));
        assertThat(request.currency()).isEqualTo("USD");
        assertThat(request.timestamp()).isEqualTo(RECEIVED_AT);
    }

    @Test
    @DisplayName("scores the debited account, not the credited one")
    void toTransactionRequest_usesDebitedAccount() {
        TransactionRequest request =
                LedgerTransferMapper.toTransactionRequest(event(100L, "USD"), RECEIVED_AT);

        assertThat(request.accountId()).isEqualTo("1001");
    }

    @Test
    @DisplayName("fills merchant and location with sentinels that match no risk list")
    void toTransactionRequest_syntheticFieldsAreExplicit() {
        TransactionRequest request =
                LedgerTransferMapper.toTransactionRequest(event(100L, "USD"), RECEIVED_AT);

        assertThat(request.merchantId()).isEqualTo(LedgerTransferMapper.SYNTHETIC_MERCHANT_ID);
        assertThat(request.location()).isEqualTo(LedgerTransferMapper.SYNTHETIC_LOCATION);
        // Bean validation on TransactionRequest requires both to be non-blank.
        assertThat(request.merchantId()).isNotBlank();
        assertThat(request.location()).isNotBlank();
    }

    @ParameterizedTest
    @CsvSource({
            "USD, 12345, 123.45",
            "EUR, 100,   1.00",
            "JPY, 12345, 12345",
            "KRW, 5000,  5000"
    })
    @DisplayName("converts minor units using the currency's own exponent")
    void toTransactionRequest_respectsCurrencyExponent(
            String currency, long minor, BigDecimal expected) {
        TransactionRequest request =
                LedgerTransferMapper.toTransactionRequest(event(minor, currency), RECEIVED_AT);

        assertThat(request.amount()).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("falls back to two decimals for an unrecognized currency code")
    void toTransactionRequest_unknownCurrency_fallsBack() {
        TransactionRequest request =
                LedgerTransferMapper.toTransactionRequest(event(12_345L, "XYZ"), RECEIVED_AT);

        assertThat(request.amount()).isEqualByComparingTo(new BigDecimal("123.45"));
    }

    @Test
    @DisplayName("rejects an event missing a field required for scoring")
    void toTransactionRequest_missingAmount_throws() {
        LedgerTransferEvent incomplete = new LedgerTransferEvent(42L, 1001L, 2002L, null, "USD");

        assertThatThrownBy(() -> LedgerTransferMapper.toTransactionRequest(incomplete, RECEIVED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amountMinor");
    }
}
