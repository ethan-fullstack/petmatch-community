package com.petmatch.community.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petmatch.community.dto.supportapplication.SupportApplicationForm;
import com.petmatch.community.exception.SupportApplicationNotFoundException;
import com.petmatch.community.exception.SupportApplicationRuleException;
import com.petmatch.community.exception.SupportApplicationStateException;
import com.petmatch.community.exception.SupportRequestNotFoundException;
import com.petmatch.community.model.SupportApplication;
import com.petmatch.community.model.SupportRequest;
import com.petmatch.community.model.User;
import com.petmatch.community.model.enums.SupportApplicationStatus;
import com.petmatch.community.model.enums.SupportRequestStatus;
import com.petmatch.community.repository.SupportApplicationRepository;
import com.petmatch.community.repository.SupportRequestRepository;

@Service
public class SupportApplicationService {

    private final SupportApplicationRepository supportApplicationRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final UserService userService;

    public SupportApplicationService(
        SupportApplicationRepository supportApplicationRepository,
        SupportRequestRepository supportRequestRepository,
        UserService userService
    ) {
        this.supportApplicationRepository = supportApplicationRepository;
        this.supportRequestRepository = supportRequestRepository;
        this.userService = userService;
    }

    @Transactional
    public SupportApplication apply(Long requestId, SupportApplicationForm form, Authentication authentication) {
        User applicant = userService.getCurrentUser(authentication);
        SupportRequest request = supportRequestRepository.findById(requestId)
            .orElseThrow(() -> new SupportRequestNotFoundException(requestId));

        if (request.getStatus() != SupportRequestStatus.OPEN || !request.getServiceDate().isAfter(LocalDateTime.now())) {
            throw new SupportApplicationRuleException("La solicitud ya no acepta postulaciones.");
        }
        if (request.getOwner().getId().equals(applicant.getId())) {
            throw new SupportApplicationRuleException("No puedes postularte a tu propia solicitud.");
        }
        if (supportApplicationRepository.existsByApplicantIdAndSupportRequestId(applicant.getId(), requestId)) {
            throw new SupportApplicationRuleException("Ya te postulaste a esta solicitud.");
        }

        return supportApplicationRepository.save(
            new SupportApplication(normalizeNullable(form.getMessage()), applicant, request)
        );
    }

    @Transactional(readOnly = true)
    public List<SupportApplication> findCurrentUserApplications(Authentication authentication) {
        User applicant = userService.getCurrentUser(authentication);
        return supportApplicationRepository.findByApplicantIdOrderByAppliedAtDesc(applicant.getId());
    }

    @Transactional(readOnly = true)
    public List<SupportApplication> findReceivedApplications(Long requestId, Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        SupportRequest request = supportRequestRepository.findByIdAndOwnerId(requestId, owner.getId())
            .orElseThrow(() -> new SupportRequestNotFoundException(requestId));
        return supportApplicationRepository.findBySupportRequestIdOrderByAppliedAtAsc(request.getId());
    }

    @Transactional
    public void accept(Long applicationId, Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        SupportApplication application = supportApplicationRepository
            .findByIdAndSupportRequestOwnerId(applicationId, owner.getId())
            .orElseThrow(() -> new SupportApplicationNotFoundException(applicationId));

        SupportRequest request = supportRequestRepository.findByIdForUpdate(application.getSupportRequest().getId())
            .orElseThrow(() -> new SupportRequestNotFoundException(application.getSupportRequest().getId()));

        if (request.getStatus() != SupportRequestStatus.OPEN
            || application.getStatus() != SupportApplicationStatus.PENDING) {
            throw new SupportApplicationStateException(applicationId);
        }
        if (supportApplicationRepository.countBySupportRequestIdAndStatus(
            request.getId(),
            SupportApplicationStatus.ACCEPTED
        ) > 0) {
            throw new SupportApplicationStateException(applicationId);
        }

        application.setStatus(SupportApplicationStatus.ACCEPTED);
        request.setStatus(SupportRequestStatus.IN_PROGRESS);

        supportApplicationRepository.findBySupportRequestIdAndStatus(
            request.getId(),
            SupportApplicationStatus.PENDING
        )
            .stream()
            .filter(other -> !other.getId().equals(applicationId))
            .forEach(other -> other.setStatus(SupportApplicationStatus.REJECTED));
    }

    @Transactional
    public void reject(Long applicationId, Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        SupportApplication application = supportApplicationRepository
            .findByIdAndSupportRequestOwnerId(applicationId, owner.getId())
            .orElseThrow(() -> new SupportApplicationNotFoundException(applicationId));

        if (application.getSupportRequest().getStatus() != SupportRequestStatus.OPEN
            || application.getStatus() != SupportApplicationStatus.PENDING) {
            throw new SupportApplicationStateException(applicationId);
        }
        application.setStatus(SupportApplicationStatus.REJECTED);
    }

    @Transactional(readOnly = true)
    public boolean hasApplied(Long requestId, Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        return supportApplicationRepository.existsByApplicantIdAndSupportRequestId(user.getId(), requestId);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
