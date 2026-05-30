package com.fraudengine.domain.ports;

/**
 * Outbound port answering whether a location is high-risk. Backed by a Redis
 * set in the infrastructure layer.
 */
public interface LocationRiskPort {

    /**
     * @param location location to check
     * @return true if the location is classified high-risk
     */
    boolean isHighRisk(String location);
}
