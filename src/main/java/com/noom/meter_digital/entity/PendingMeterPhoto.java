package com.noom.meter_digital.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_meter_photos")
public class PendingMeterPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String photoId;

    private String roomNumber;
    private String imagePath;
    private String originalFileName;

    @Column(nullable = false)
    private String status; // PENDING_APPROVAL, APPROVED, REJECTED

    private Double detectedReading;
    private String rawText;

    private Boolean anomalyDetected = false;
    private String anomalyType;
    private String warningMessage;

    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public PendingMeterPhoto() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING_APPROVAL";
    }

    public PendingMeterPhoto(String photoId, String roomNumber, String imagePath, String originalFileName) {
        this.photoId = photoId;
        this.roomNumber = roomNumber;
        this.imagePath = imagePath;
        this.originalFileName = originalFileName;
        this.status = "PENDING_APPROVAL";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPhotoId() { return photoId; }
    public void setPhotoId(String photoId) { this.photoId = photoId; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getDetectedReading() { return detectedReading; }
    public void setDetectedReading(Double detectedReading) { this.detectedReading = detectedReading; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public Boolean getAnomalyDetected() { return anomalyDetected; }
    public void setAnomalyDetected(Boolean anomalyDetected) { this.anomalyDetected = anomalyDetected; }
    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
}
