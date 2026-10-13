package com.noom.meter_digital.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.noom.meter_digital.model.modbus.ModbusProfile;
import com.noom.meter_digital.model.modbus.RegisterMapping;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class ModbusProfileManagerService {

    private static final Logger log = LoggerFactory.getLogger(ModbusProfileManagerService.class);

    private final Map<String, ModbusProfile> profileMap = new HashMap<>();
    private final ObjectMapper objectMapper;
    private final File configDir;

    public ModbusProfileManagerService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.configDir = new File("./config/modbus-profiles");
    }

    @PostConstruct
    public void init() {
        if (!configDir.exists()) {
            boolean created = configDir.mkdirs();
            if (created) {
                log.info("Created Modbus dynamic profile directory: {}", configDir.getAbsolutePath());
            }
        }

        // Initialize default built-in profiles if not present on disk
        initDefaultProfiles();

        // Load all profiles from disk directory
        loadProfilesFromDisk();
    }

    private void initDefaultProfiles() {
        Map<String, ModbusProfile> defaults = createBuiltInProfiles();
        for (Map.Entry<String, ModbusProfile> entry : defaults.entrySet()) {
            String modelKey = entry.getKey();
            ModbusProfile profile = entry.getValue();
            File profileFile = new File(configDir, modelKey + ".json");
            if (!profileFile.exists()) {
                try {
                    objectMapper.writeValue(profileFile, profile);
                    log.info("Initialized default Modbus profile JSON: {}", profileFile.getName());
                } catch (IOException e) {
                    log.error("Failed to write default profile {}: {}", modelKey, e.getMessage());
                }
            }
        }
    }

    public synchronized void loadProfilesFromDisk() {
        profileMap.clear();

        // First load built-ins as fallback memory defaults
        Map<String, ModbusProfile> builtIns = createBuiltInProfiles();
        profileMap.putAll(builtIns);

        // Load disk JSON files (overrides built-ins if saved by user)
        if (configDir.exists() && configDir.isDirectory()) {
            File[] jsonFiles = configDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (jsonFiles != null) {
                for (File file : jsonFiles) {
                    try {
                        ModbusProfile profile = objectMapper.readValue(file, ModbusProfile.class);
                        if (profile != null && profile.getModelKey() != null && !profile.getModelKey().isBlank()) {
                            profileMap.put(profile.getModelKey().toUpperCase().trim(), profile);
                            log.info("Loaded dynamic Modbus profile [{}] from {}", profile.getModelKey(), file.getName());
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse Modbus profile JSON {}: {}", file.getName(), e.getMessage());
                    }
                }
            }
        }
    }

    public List<ModbusProfile> getAllProfiles() {
        return new ArrayList<>(profileMap.values());
    }

    public ModbusProfile getProfile(String modelKey) {
        if (modelKey == null || modelKey.isBlank()) {
            return profileMap.get("MODEL_A");
        }
        ModbusProfile profile = profileMap.get(modelKey.toUpperCase().trim());
        if (profile == null) {
            log.warn("Modbus profile [{}] not found, falling back to MODEL_A", modelKey);
            return profileMap.get("MODEL_A");
        }
        return profile;
    }

    public synchronized ModbusProfile saveProfile(ModbusProfile profile) throws IOException {
        if (profile.getModelKey() == null || profile.getModelKey().isBlank()) {
            throw new IllegalArgumentException("modelKey is required");
        }
        String key = profile.getModelKey().toUpperCase().trim();
        profile.setModelKey(key);

        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File file = new File(configDir, key + ".json");
        objectMapper.writeValue(file, profile);

        profileMap.put(key, profile);
        log.info("Successfully saved dynamic Modbus profile [{}] to {}", key, file.getAbsolutePath());
        return profile;
    }

    public synchronized boolean deleteProfile(String modelKey) {
        if (modelKey == null || modelKey.isBlank()) return false;
        String key = modelKey.toUpperCase().trim();

        ModbusProfile profile = profileMap.get(key);
        if (profile != null && profile.isBuiltIn()) {
            log.warn("Cannot delete built-in profile [{}]", key);
            // Allow override but keep built-in fallback
        }

        File file = new File(configDir, key + ".json");
        boolean deleted = false;
        if (file.exists()) {
            deleted = file.delete();
        }

        profileMap.remove(key);
        // Reload built-ins if available
        Map<String, ModbusProfile> builtIns = createBuiltInProfiles();
        if (builtIns.containsKey(key)) {
            profileMap.put(key, builtIns.get(key));
        }

        return deleted;
    }

    private Map<String, ModbusProfile> createBuiltInProfiles() {
        Map<String, ModbusProfile> map = new LinkedHashMap<>();

        // 1. Eastron SDM120 (Single Phase)
        ModbusProfile sdm120 = new ModbusProfile("EASTRON_SDM120", "Eastron", "SDM120 Single Phase Smart Meter", "Eastron SDM120 Series (Float32 CD_AB, Input Registers 0x04)", 4, "AUTO");
        sdm120.setBuiltIn(true);
        sdm120.addMapping(new RegisterMapping("voltage", 0, 2, "FLOAT32_CD_AB", 1.0, "V"));
        sdm120.addMapping(new RegisterMapping("current", 6, 2, "FLOAT32_CD_AB", 1.0, "A"));
        sdm120.addMapping(new RegisterMapping("power", 12, 2, "FLOAT32_CD_AB", 1.0, "kW"));
        sdm120.addMapping(new RegisterMapping("energyKwh", 342, 2, "FLOAT32_CD_AB", 1.0, "kWh"));
        map.put(sdm120.getModelKey(), sdm120);

        // 2. Eastron SDM630 (Three Phase)
        ModbusProfile sdm630 = new ModbusProfile("EASTRON_SDM630", "Eastron", "SDM630 Three Phase Smart Meter", "Eastron SDM630 Series (Float32 CD_AB, Input Registers 0x04)", 4, "AUTO");
        sdm630.setBuiltIn(true);
        sdm630.addMapping(new RegisterMapping("voltage", 0, 2, "FLOAT32_CD_AB", 1.0, "V"));
        sdm630.addMapping(new RegisterMapping("current", 6, 2, "FLOAT32_CD_AB", 1.0, "A"));
        sdm630.addMapping(new RegisterMapping("power", 52, 2, "FLOAT32_CD_AB", 1.0, "kW"));
        sdm630.addMapping(new RegisterMapping("energyKwh", 342, 2, "FLOAT32_CD_AB", 1.0, "kWh"));
        map.put(sdm630.getModelKey(), sdm630);

        // 3. Schneider PM2200 / PM2100
        ModbusProfile pm2200 = new ModbusProfile("SCHNEIDER_PM2200", "Schneider Electric", "PM2200 Power Meter", "Schneider METSEPM2200 Series (Float32 AB_CD, Holding Registers 0x03)", 3, "AUTO");
        pm2200.setBuiltIn(true);
        pm2200.addMapping(new RegisterMapping("voltage", 3027, 2, "FLOAT32_AB_CD", 1.0, "V"));
        pm2200.addMapping(new RegisterMapping("current", 2999, 2, "FLOAT32_AB_CD", 1.0, "A"));
        pm2200.addMapping(new RegisterMapping("power", 3059, 2, "FLOAT32_AB_CD", 0.001, "kW"));
        pm2200.addMapping(new RegisterMapping("energyKwh", 3203, 2, "FLOAT32_AB_CD", 1.0, "kWh"));
        map.put(pm2200.getModelKey(), pm2200);

        // 4. Circutor CVM-C5 / CVM-10
        ModbusProfile cvmc5 = new ModbusProfile("CIRCUTOR_CVMC5", "Circutor", "CVM-C5 Analyzer", "Circutor CVM-C5 Series (UInt32 AB_CD, Holding Registers 0x03)", 3, "AUTO");
        cvmc5.setBuiltIn(true);
        cvmc5.addMapping(new RegisterMapping("voltage", 0, 2, "UINT32_AB_CD", 0.1, "V"));
        cvmc5.addMapping(new RegisterMapping("current", 2, 2, "UINT32_AB_CD", 0.001, "A"));
        cvmc5.addMapping(new RegisterMapping("power", 4, 2, "UINT32_AB_CD", 0.001, "kW"));
        cvmc5.addMapping(new RegisterMapping("energyKwh", 108, 2, "UINT32_AB_CD", 0.001, "kWh")); // Reg 108 (0x006C) = Tariff 2 (T2) Energy
        map.put(cvmc5.getModelKey(), cvmc5);

        // 5. Chint DTSU666
        ModbusProfile chint = new ModbusProfile("CHINT_DTSU666", "Chint", "DTSU666 Smart Energy Meter", "Chint DTSU666 Series (Holding Registers 0x03)", 3, "AUTO");
        chint.setBuiltIn(true);
        chint.addMapping(new RegisterMapping("voltage", 8192, 2, "FLOAT32_CD_AB", 0.1, "V"));
        chint.addMapping(new RegisterMapping("current", 8198, 2, "FLOAT32_CD_AB", 0.001, "A"));
        chint.addMapping(new RegisterMapping("power", 8204, 2, "FLOAT32_CD_AB", 0.001, "kW"));
        chint.addMapping(new RegisterMapping("energyKwh", 16384, 2, "FLOAT32_CD_AB", 0.1, "kWh"));
        map.put(chint.getModelKey(), chint);

        // 6. Acrel DTSD1352
        ModbusProfile acrel = new ModbusProfile("ACREL_DTSD1352", "Acrel", "DTSD1352 / SDM Meter", "Acrel Multi-function Energy Meter (Holding Registers 0x03)", 3, "AUTO");
        acrel.setBuiltIn(true);
        acrel.addMapping(new RegisterMapping("voltage", 97, 2, "FLOAT32_CD_AB", 0.1, "V"));
        acrel.addMapping(new RegisterMapping("current", 105, 2, "FLOAT32_CD_AB", 0.01, "A"));
        acrel.addMapping(new RegisterMapping("power", 119, 2, "FLOAT32_CD_AB", 0.001, "kW"));
        acrel.addMapping(new RegisterMapping("energyKwh", 0, 2, "FLOAT32_CD_AB", 0.01, "kWh"));
        map.put(acrel.getModelKey(), acrel);

        // 7. Generic MODEL_A
        ModbusProfile modelA = new ModbusProfile("MODEL_A", "Generic", "Standard Modbus Float Meter A", "Default standard float register map starting at Reg 0 (FC 0x04)", 4, "AUTO");
        modelA.setBuiltIn(true);
        modelA.addMapping(new RegisterMapping("voltage", 0, 2, "FLOAT32_AB_CD", 1.0, "V"));
        modelA.addMapping(new RegisterMapping("current", 2, 2, "FLOAT32_AB_CD", 1.0, "A"));
        modelA.addMapping(new RegisterMapping("power", 4, 2, "FLOAT32_AB_CD", 1.0, "kW"));
        modelA.addMapping(new RegisterMapping("energyKwh", 6, 2, "FLOAT32_AB_CD", 1.0, "kWh"));
        map.put(modelA.getModelKey(), modelA);

        // 8. Generic MODEL_B
        ModbusProfile modelB = new ModbusProfile("MODEL_B", "Generic", "Standard Modbus Holding Float Meter B", "Default holding float register map starting at Reg 0 (FC 0x03)", 3, "AUTO");
        modelB.setBuiltIn(true);
        modelB.addMapping(new RegisterMapping("voltage", 0, 2, "FLOAT32_AB_CD", 1.0, "V"));
        modelB.addMapping(new RegisterMapping("current", 2, 2, "FLOAT32_AB_CD", 1.0, "A"));
        modelB.addMapping(new RegisterMapping("power", 4, 2, "FLOAT32_AB_CD", 1.0, "kW"));
        modelB.addMapping(new RegisterMapping("energyKwh", 6, 2, "FLOAT32_AB_CD", 1.0, "kWh"));
        map.put(modelB.getModelKey(), modelB);

        return map;
    }
}
