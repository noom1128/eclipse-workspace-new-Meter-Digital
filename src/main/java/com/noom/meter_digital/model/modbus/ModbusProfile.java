package com.noom.meter_digital.model.modbus;

import java.util.ArrayList;
import java.util.List;

public class ModbusProfile {

    private String modelKey; // e.g., "SCHNEIDER_PM2200", "EASTRON_SDM120", "EASTRON_SDM630", "CIRCUTOR_CVMC5", "CHINT_DTSU666", "ACREL_DTSD1352"
    private String brand; // e.g. "Schneider Electric", "Eastron", "Circutor", "Chint", "Acrel"
    private String modelName; // Display name
    private String description;
    private int defaultFunctionCode = 3; // 3 = Holding Registers (0x03), 4 = Input Registers (0x04)
    private String defaultProtocolMode = "AUTO"; // AUTO, MODBUS_TCP, MODBUS_RTU_OVER_TCP
    private boolean isBuiltIn = false;

    private List<RegisterMapping> registerMappings = new ArrayList<>();

    public ModbusProfile() {}

    public ModbusProfile(String modelKey, String brand, String modelName, String description, int defaultFunctionCode, String defaultProtocolMode) {
        this.modelKey = modelKey;
        this.brand = brand;
        this.modelName = modelName;
        this.description = description;
        this.defaultFunctionCode = defaultFunctionCode;
        this.defaultProtocolMode = defaultProtocolMode;
    }

    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDefaultFunctionCode() { return defaultFunctionCode; }
    public void setDefaultFunctionCode(int defaultFunctionCode) { this.defaultFunctionCode = defaultFunctionCode; }

    public String getDefaultProtocolMode() { return defaultProtocolMode; }
    public void setDefaultProtocolMode(String defaultProtocolMode) { this.defaultProtocolMode = defaultProtocolMode; }

    public boolean isBuiltIn() { return isBuiltIn; }
    public void setBuiltIn(boolean builtIn) { isBuiltIn = builtIn; }

    public List<RegisterMapping> getRegisterMappings() { return registerMappings; }
    public void setRegisterMappings(List<RegisterMapping> registerMappings) { this.registerMappings = registerMappings; }

    public void addMapping(RegisterMapping mapping) {
        if (this.registerMappings == null) {
            this.registerMappings = new ArrayList<>();
        }
        this.registerMappings.add(mapping);
    }
}
