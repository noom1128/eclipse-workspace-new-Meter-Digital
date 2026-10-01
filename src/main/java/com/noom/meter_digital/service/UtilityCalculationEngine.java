package com.noom.meter_digital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Smart Utility Calculation Engine for Apartment Management System
 * Supports:
 * 1. Meter Rollover Protection (e.g., 9980 -> 0020 on a 4-digit meter)
 * 2. Flex-Rate Calculation (PER_UNIT, MINIMUM charge, FLAT_RATE)
 * 3. Anomaly & Consumption Spike Validation Alerts
 */
@Component
public class UtilityCalculationEngine {

    public enum RateType {
        PER_UNIT,
        MINIMUM,
        FLAT_RATE
    }

    public static class CalculationResult {
        private final double unitsUsed;
        private final double totalAmount;
        private final boolean rolloverDetected;
        private final boolean anomalyDetected;
        private final String warningMessage;

        public CalculationResult(double unitsUsed, double totalAmount, boolean rolloverDetected, boolean anomalyDetected, String warningMessage) {
            this.unitsUsed = unitsUsed;
            this.totalAmount = totalAmount;
            this.rolloverDetected = rolloverDetected;
            this.anomalyDetected = anomalyDetected;
            this.warningMessage = warningMessage;
        }

        public double getUnitsUsed() { return unitsUsed; }
        public double getTotalAmount() { return totalAmount; }
        public boolean isRolloverDetected() { return rolloverDetected; }
        public boolean isAnomalyDetected() { return anomalyDetected; }
        public String getWarningMessage() { return warningMessage; }
    }

    /**
     * Calculate units used with meter rollover guard.
     */
    public double calculateUnits(double openingReading, double closingReading, int meterDigits) {
        if (closingReading >= openingReading) {
            return round(closingReading - openingReading);
        }
        
        int digits = meterDigits > 0 ? meterDigits : 4;
        double maxLimit = Math.pow(10, digits);
        double units = (maxLimit - openingReading) + closingReading;
        return round(units);
    }

    /**
     * Calculate utility cost based on RateType (PER_UNIT, MINIMUM, FLAT_RATE).
     */
    public CalculationResult calculateCost(double openingReading, double closingReading, RateType rateType, double ratePerUnit, double minCharge, int meterDigits, Double averageMonthlyUnits) {
        boolean rollover = closingReading < openingReading;
        double unitsUsed = calculateUnits(openingReading, closingReading, meterDigits);

        double totalAmount = 0.0;
        RateType type = rateType != null ? rateType : RateType.PER_UNIT;

        switch (type) {
            case FLAT_RATE:
                totalAmount = round(minCharge > 0 ? minCharge : ratePerUnit);
                break;

            case MINIMUM:
                double calculatedCost = unitsUsed * ratePerUnit;
                totalAmount = round(Math.max(calculatedCost, minCharge));
                break;

            case PER_UNIT:
            default:
                totalAmount = round(unitsUsed * ratePerUnit);
                break;
        }

        boolean anomaly = false;
        String warning = null;
        if (averageMonthlyUnits != null && averageMonthlyUnits > 0 && unitsUsed > (3.0 * averageMonthlyUnits)) {
            anomaly = true;
            warning = "High consumption alert: Units used (" + unitsUsed + ") is more than 3x monthly average (" + averageMonthlyUnits + ")";
        }

        return new CalculationResult(unitsUsed, totalAmount, rollover, anomaly, warning);
    }

    private double round(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
