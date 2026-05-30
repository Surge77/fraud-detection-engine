package com.fraudengine.domain.ports;

/**
 * Outbound port answering whether a merchant is blacklisted. Backed by a Redis
 * set in the infrastructure layer.
 */
public interface MerchantBlacklistPort {

    /**
     * @param merchantId merchant to check
     * @return true if the merchant is blacklisted
     */
    boolean isBlacklisted(String merchantId);
}
