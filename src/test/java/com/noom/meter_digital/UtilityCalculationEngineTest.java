package com.noom.meter_digital;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import com.noom.meter_digital.service.UtilityCalculationEngine;

public class UtilityCalculationEngineTest {

    private final UtilityCalculationEngine engine = new UtilityCalculationEngine();

    @Test
    void testNormalReadingDifference() {
        double units = engine.calculateUnits(150.0, 200.0, 4);
        assertEquals(50.0, units);
    }

    @Test
    void testAnalogMeter4DigitRollover() {
        // e.g. 9999 -> 1
        double units = engine.calculateUnits(9999.0, 1.0, 4);
        assertEquals(2.0, units);
    }

    @Test
    void testAnalogMeter4DigitWithDecimalRollover() {
        // e.g. 9999.9 -> 1.0
        double units = engine.calculateUnits(9999.9, 1.0, 4);
        assertEquals(1.1, units);
    }

    @Test
    void testAnalogMeter5DigitRollover() {
        // e.g. 99998 -> 3 on 5-digit meter
        double units = engine.calculateUnits(99998.0, 3.0, 5);
        assertEquals(5.0, units);
    }

    @Test
    void testDigitalMeter6DigitRollover() {
        // e.g. 999998.5 -> 2.5 on 6-digit meter
        double units = engine.calculateUnits(999998.5, 2.5, 6);
        assertEquals(4.0, units);
    }

    @Test
    void testDigitalMeter9DigitRolloverAutoScaling() {
        // e.g. 999,999,998 -> 2 on 9-digit digital meter
        double units = engine.calculateUnits(999999998.0, 2.0, 9);
        assertEquals(4.0, units);

        // Even if configured as 4 digits, auto-scale handles 9 digits safely!
        double unitsAutoscaled = engine.calculateUnits(999999998.0, 2.0, 4);
        assertEquals(4.0, unitsAutoscaled);
    }

    @Test
    void testCostCalculationWithRolloverFlag() {
        UtilityCalculationEngine.CalculationResult result = engine.calculateCost(
                9999.0, 1.0, UtilityCalculationEngine.RateType.PER_UNIT, 7.0, 0.0, 4, 100.0
        );
        assertTrue(result.isRolloverDetected());
        assertEquals(2.0, result.getUnitsUsed());
        assertEquals(14.0, result.getTotalAmount()); // 2 units * 7 baht = 14 baht
        assertNotNull(result.getWarningMessage());
        assertTrue(result.getWarningMessage().contains("หมุนครบรอบ"));
    }
}
