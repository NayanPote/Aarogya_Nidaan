package com.healthcare.aarogyanidaan;

import java.util.Date;

public class HealthData {
    private String patientId;
    private String dataType;
    private String value;
    private Date timestamp;
    private boolean isManualInput;

    public HealthData(String patientId, String dataType, String value, boolean isManualInput) {
        this.patientId = patientId;
        this.dataType = dataType;
        this.value = value;
        this.timestamp = new Date();
        this.isManualInput = isManualInput;
    }

    // Getters and Setters
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDataType() { return dataType; }
    public String getValue() { return value; }
    public Date getTimestamp() { return timestamp; }
    public boolean isManualInput() { return isManualInput; }
}