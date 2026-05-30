package com.fraudengine.domain.model;

/**
 * A merchant and the number of times its transactions were flagged.
 *
 * @param merchantId merchant identifier
 * @param flagCount  number of blocked transactions for the merchant
 */
public record MerchantFlagCount(String merchantId, long flagCount) {
}
