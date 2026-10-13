package com.noom.meter_digital;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.noom.meter_digital.entity.MeterReading;
import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.repository.MeterReadingRepository;
import com.noom.meter_digital.repository.RoomRepository;
import com.noom.meter_digital.service.MeterAnomalyService;

@ExtendWith(MockitoExtension.class)
public class MeterAnomalyServiceTest {

    @Mock
    private MeterReadingRepository meterReadingRepository;

    @Mock
    private RoomRepository roomRepository;

    private MeterAnomalyService anomalyService;

    @BeforeEach
    void setUp() {
        anomalyService = new MeterAnomalyService(meterReadingRepository, roomRepository);
    }

    @Test
    void testNormalReadingConsumption() {
        Room room = new Room();
        room.setId(101L);
        room.setRoomNumber("101");
        when(roomRepository.findAllByRoomNumberOrderByIdAsc("101")).thenReturn(List.of(room));

        MeterReading prev = new MeterReading();
        prev.setUnit(100.0);
        when(meterReadingRepository.findTopByRoomIdOrderByTimestampDesc(101L)).thenReturn(prev);

        MeterAnomalyService.AnomalyResult result = anomalyService.analyzeReading("101", 110.0, null);

        assertFalse(result.isAnomalyDetected());
        assertEquals("NORMAL", result.getAnomalyType());
        assertEquals(10.0, result.getDeltaUnits());
        assertEquals("NONE", result.getSeverity());
    }

    @Test
    void testExtremeSpikeAnomaly() {
        Room room = new Room();
        room.setId(101L);
        room.setRoomNumber("101");
        when(roomRepository.findAllByRoomNumberOrderByIdAsc("101")).thenReturn(List.of(room));

        MeterReading prev = new MeterReading();
        prev.setUnit(100.0);
        when(meterReadingRepository.findTopByRoomIdOrderByTimestampDesc(101L)).thenReturn(prev);

        // 100 -> 5,000 (+4,900 units jump!)
        MeterAnomalyService.AnomalyResult result = anomalyService.analyzeReading("101", 5000.0, null);

        assertTrue(result.isAnomalyDetected());
        assertEquals("EXTREME_SPIKE", result.getAnomalyType());
        assertEquals(4900.0, result.getDeltaUnits());
        assertEquals("CRITICAL", result.getSeverity());
        assertTrue(result.isRequiresConfirmation());
        assertTrue(result.getWarningMessage().contains("กระโดดผิดปกติ"));
    }

    @Test
    void testDecimalSlipDetection() {
        Room room = new Room();
        room.setId(101L);
        room.setRoomNumber("101");
        when(roomRepository.findAllByRoomNumberOrderByIdAsc("101")).thenReturn(List.of(room));

        MeterReading prev = new MeterReading();
        prev.setUnit(100.0);
        when(meterReadingRepository.findTopByRoomIdOrderByTimestampDesc(101L)).thenReturn(prev);

        // Actual meter is 115.0, but OCR read 1150 (forgot decimal point, delta = 1050)
        // 1150 / 10 = 115.0 -> delta is +15.0 which is normal!
        MeterAnomalyService.AnomalyResult result = anomalyService.analyzeReading("101", 1150.0, null);

        assertTrue(result.isAnomalyDetected());
        assertEquals("DECIMAL_SLIP", result.getAnomalyType());
        assertEquals(115.0, result.getSuggestedReading());
        assertTrue(result.getWarningMessage().contains("อาจลืมจุดทศนิยม"));
    }

    @Test
    void testAbnormalDropDetection() {
        Room room = new Room();
        room.setId(101L);
        room.setRoomNumber("101");
        when(roomRepository.findAllByRoomNumberOrderByIdAsc("101")).thenReturn(List.of(room));

        MeterReading prev = new MeterReading();
        prev.setUnit(500.0);
        when(meterReadingRepository.findTopByRoomIdOrderByTimestampDesc(101L)).thenReturn(prev);

        // Reading dropped from 500 to 200 (-300 units) without rollover
        MeterAnomalyService.AnomalyResult result = anomalyService.analyzeReading("101", 200.0, null);

        assertTrue(result.isAnomalyDetected());
        assertEquals("ABNORMAL_DROP", result.getAnomalyType());
        assertEquals("HIGH", result.getSeverity());
        assertTrue(result.getWarningMessage().contains("ลดลงผิดปกติ"));
    }

    @Test
    void testRoomTagMismatchDetection() {
        // Target room is 101, but Gemini sees "102" on the meter photo
        MeterAnomalyService.AnomalyResult result = anomalyService.analyzeReading("101", 110.0, "102");

        assertTrue(result.isAnomalyDetected());
        assertEquals("ROOM_MISMATCH", result.getAnomalyType());
        assertEquals("CRITICAL", result.getSeverity());
        assertTrue(result.getWarningMessage().contains("ป้ายห้องในภาพไม่ตรงกัน"));
        assertTrue(result.getWarningMessage().contains("102"));
    }
}
