package com.healthcare.aarogyanidaan;

public class ChatRequest {
    private String requestId;
    private String patientId;
    private String doctorId;
    private String patientName;
    private long timestamp;
    private String status;

    public ChatRequest() {
        // Required empty constructor for Firebase
    }

    public ChatRequest(String requestId, String patientId, String doctorId,
                       String patientName, long timestamp, String status) {
        this.requestId = requestId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.patientName = patientName;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters and setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
