package com.petmatch.community.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.petmatch.community.model.SupportApplication;
import com.petmatch.community.model.enums.SupportApplicationStatus;

public interface SupportApplicationRepository extends JpaRepository<SupportApplication, Long> {

    @EntityGraph(attributePaths = {"applicant", "supportRequest", "supportRequest.pet", "supportRequest.owner"})
    List<SupportApplication> findBySupportRequestIdOrderByAppliedAtAsc(Long supportRequestId);

    @EntityGraph(attributePaths = {"applicant", "supportRequest", "supportRequest.pet", "supportRequest.owner"})
    List<SupportApplication> findByApplicantIdOrderByAppliedAtDesc(Long applicantId);

    @EntityGraph(attributePaths = {"applicant", "supportRequest", "supportRequest.pet", "supportRequest.owner"})
    Optional<SupportApplication> findByIdAndSupportRequestOwnerId(Long id, Long ownerId);

    Optional<SupportApplication> findByApplicantIdAndSupportRequestId(
        Long applicantId,
        Long supportRequestId
    );

    boolean existsByApplicantIdAndSupportRequestId(Long applicantId, Long supportRequestId);

    long countBySupportRequestIdAndStatus(
        Long supportRequestId,
        SupportApplicationStatus status
    );

    List<SupportApplication> findBySupportRequestIdAndStatus(
        Long supportRequestId,
        SupportApplicationStatus status
    );
}
