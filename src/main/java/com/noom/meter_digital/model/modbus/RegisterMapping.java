package com.noom.meter_digital.model.modbus;

public class RegisterMapping {

    private String metric; // "voltage", "current", "power", "energyKwh", "frequency", "powerFactor"
    private int registerAddress; // e.g. 0, 30001, 3054
    private int registerCount = 2; // 1 for 16-bit, 2 for 32-bit float/int
    private String dataType = "FLOAT32_AB_CD"; // FLOAT32_AB_CD, FLOAT32_CD_AB, FLOAT32_BA_DC, FLOAT32_DC_BA, INT32_AB_CD, INT32_CD_AB, UINT32, INT16, UINT16
    private double multiplier = 1.0;
    private String unit = "";

    public RegisterMapping() {}

    public RegisterMapping(String metric, int registerAddress, int registerCount, String dataType, double multiplier, String unit) {
        this.metric = metric;
        this.registerAddress = registerAddress;
        this.registerCount = registerCount;
        this.dataType = dataType;
        this.multiplier = multiplier;
        this.unit = unit;
    }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public int getRegisterAddress() { return registerAddress; }
    public void setRegisterAddress(int registerAddress) { this.registerAddress = registerAddress; }

    public int getRegisterCount() { return registerCount; }
    public void setRegisterCount(int registerCount) { this.registerCount = registerCount; }

    public String getDataType() { return dataType != null ? dataType : "FLOAT32_AB_CD"; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public double getMultiplier() { return multiplier != 0 ? multiplier : 1.0; }
    public void setMultiplier(double multiplier) { this.multiplier = multiplier; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
