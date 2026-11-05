// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.api.entity.compliance;

import dev.strata.common.data.entity.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** KYC real-name verification application row ({@code kyc_application}). */
@Entity
@Table(
        name = "kyc_application",
        indexes = {
            @Index(name = "idx_kyc_user_id", columnList = "user_id"),
            @Index(name = "idx_kyc_status", columnList = "status")
        })
public class KycApplication extends TimestampedEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    /** id_card | passport. */
    @Column(name = "id_type", nullable = false, length = 16)
    private String idType;

    @Column(name = "id_number_hash", nullable = false, length = 128)
    private String idNumberHash;

    @Column(name = "front_image_url", length = 512)
    private String frontImageUrl;

    @Column(name = "back_image_url", length = 512)
    private String backImageUrl;

    @Column(name = "selfie_url", length = 512)
    private String selfieUrl;

    /** pending | approved | rejected. */
    @Column(nullable = false, length = 16)
    private String status = "pending";

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reject_reason", length = 512)
    private String rejectReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getIdNumberHash() {
        return idNumberHash;
    }

    public void setIdNumberHash(String idNumberHash) {
        this.idNumberHash = idNumberHash;
    }

    public String getFrontImageUrl() {
        return frontImageUrl;
    }

    public void setFrontImageUrl(String frontImageUrl) {
        this.frontImageUrl = frontImageUrl;
    }

    public String getBackImageUrl() {
        return backImageUrl;
    }

    public void setBackImageUrl(String backImageUrl) {
        this.backImageUrl = backImageUrl;
    }

    public String getSelfieUrl() {
        return selfieUrl;
    }

    public void setSelfieUrl(String selfieUrl) {
        this.selfieUrl = selfieUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
