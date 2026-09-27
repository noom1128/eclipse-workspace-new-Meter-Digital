package com.noom.meter_digital.service;

import com.noom.meter_digital.entity.MeterReading;
import com.noom.meter_digital.repository.MeterReadingRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MeterReadingService {

    private final MeterReadingRepository repository;
    
    @Autowired
    private MeterReadingRepository meterRepo;

    public MeterReadingService(MeterReadingRepository repository) {
        this.repository = repository;
    }

    public MeterReading save(MeterReading reading) {
        return repository.save(reading);
    }

    public List<MeterReading> getAll() {
        return repository.findAll();
    }
}
