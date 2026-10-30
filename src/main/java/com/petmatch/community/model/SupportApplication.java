package com.petmatch.community.model;

import java.time.LocalDateTime;

import com.petmatch.community.model.enums.SupportApplicationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(
    name = "support_applications",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_support_applications_applicant_request",
        columnNames = {"applicant_id", "support_request_id"}
    )
)
public class SupportApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 1000)
    @Column(length = 1000)
    private String message;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportApplicationStatus status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "support_request_id", nullable = false)
    private SupportRequest supportRequest;

    protected SupportApplication() {
    }

    public SupportApplication(String message, User applicant, SupportRequest supportRequest) {
        this.message = message;
        this.applicant = applicant;
        this.supportRequest = supportRequest;
    }

    @PrePersist
    void initializeDefaults() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SupportApplicationStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public SupportApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(SupportApplicationStatus status) {
        this.status = status;
    }

    public User getApplicant() {
        return applicant;
    }

    public void setApplicant(User applicant) {
        this.applicant = applicant;
    }

    public SupportRequest getSupportRequest() {
        return supportRequest;
    }

    public void setSupportRequest(SupportRequest supportRequest) {
        this.supportRequest = supportRequest;
    }
}
