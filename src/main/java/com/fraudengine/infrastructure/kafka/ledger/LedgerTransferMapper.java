package com.fraudengine.infrastructure.kafka.ledger;

import com.fraudengine.domain.model.TransactionRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

/**
 * Anti-corruption layer between ledger-engine and this service.
 *
 * <p>The two domains do not align: a ledger transfer moves value between two internal
 * accounts, while this engine scores merchant-facing transactions and requires
 * {@code merchantId} and {@code location}. Rather than polluting the ledger with
 * merchant concepts or relaxing our own validation, the translation is owned here,
 * at the boundary where the mismatch actually lives.
 *
 * <p><strong>Synthetic fields.</strong> {@code merchantId} and {@code location} are
 * filled with explicit sentinels, not invented business data. They deliberately match
 * no blacklist and no high-risk location, so a ledger transfer is scored on velocity
 * and amount alone. Anything else would be fabricated signal.
 */
public final class LedgerTransferMapper {

    /** Marks a transaction as originating from the ledger, not a merchant terminal. */
    public static final String SYNTHETIC_MERCHANT_ID = "ledger:internal-transfer";
    public static final String SYNTHETIC_MERCHANT_NAME = "Internal ledger transfer";
    public static final String SYNTHETIC_LOCATION = "INTERNAL";

    private static final int FALLBACK_FRACTION_DIGITS = 2;

    private LedgerTransferMapper() {
    }

    /**
     * Translates a ledger event into a scorable transaction.
     *
     * @param event    the ledger's own domain event
     * @param receivedAt time this event was consumed, used as the transaction timestamp
     *                   because the ledger payload carries no event time; velocity
     *                   windows are therefore measured from receipt
     * @return a request this engine's pipeline can evaluate
     * @throws IllegalArgumentException if a field required for scoring is absent
     */
    public static TransactionRequest toTransactionRequest(LedgerTransferEvent event, Instant receivedAt) {
        require(event.transactionId() != null, "transactionId");
        require(event.from() != null, "from");
        require(event.amountMinor() != null, "amountMinor");
        require(event.currency() != null && !event.currency().isBlank(), "currency");

        return new TransactionRequest(
                Long.toString(event.transactionId()),
                Long.toString(event.from()),
                toMajorUnits(event.amountMinor(), event.currency()),
                event.currency(),
                SYNTHETIC_MERCHANT_ID,
                SYNTHETIC_MERCHANT_NAME,
                SYNTHETIC_LOCATION,
                receivedAt);
    }

    /**
     * Converts minor units using the currency's own exponent rather than assuming
     * two decimals — JPY and KRW have none, so a fixed divide-by-100 would inflate
     * every yen amount by a factor of 100.
     */
    private static BigDecimal toMajorUnits(long amountMinor, String currencyCode) {
        int fractionDigits;
        try {
            fractionDigits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
        } catch (IllegalArgumentException unknownCurrency) {
            fractionDigits = FALLBACK_FRACTION_DIGITS;
        }
        if (fractionDigits < 0) {
            fractionDigits = FALLBACK_FRACTION_DIGITS;
        }
        return BigDecimal.valueOf(amountMinor, fractionDigits);
    }

    private static void require(boolean condition, String field) {
        if (!condition) {
            throw new IllegalArgumentException("ledger transfer event missing required field: " + field);
        }
    }
}
