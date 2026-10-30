package com.petmatch.community.service;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petmatch.community.dto.supportrequest.SupportRequestForm;
import com.petmatch.community.exception.SupportRequestNotFoundException;
import com.petmatch.community.exception.SupportRequestStateException;
import com.petmatch.community.model.Pet;
import com.petmatch.community.model.SupportRequest;
import com.petmatch.community.model.User;
import com.petmatch.community.model.enums.SupportApplicationStatus;
import com.petmatch.community.model.enums.SupportRequestStatus;
import com.petmatch.community.repository.SupportApplicationRepository;
import com.petmatch.community.repository.SupportRequestRepository;

@Service
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;
    private final SupportApplicationRepository supportApplicationRepository;
    private final PetService petService;
    private final UserService userService;
    private final EntityManager entityManager;

    public SupportRequestService(
        SupportRequestRepository supportRequestRepository,
        SupportApplicationRepository supportApplicationRepository,
        PetService petService,
        UserService userService,
        EntityManager entityManager
    ) {
        this.supportRequestRepository = supportRequestRepository;
        this.supportApplicationRepository = supportApplicationRepository;
        this.petService = petService;
        this.userService = userService;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<SupportRequest> findOpenRequests() {
        return supportRequestRepository.findByStatusAndServiceDateAfterOrderByServiceDateAsc(
            SupportRequestStatus.OPEN,
            LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<SupportRequest> findCurrentUserRequests(Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        return supportRequestRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId());
    }

    @Transactional(readOnly = true)
    public SupportRequest findById(Long requestId) {
        return supportRequestRepository.findById(requestId)
            .orElseThrow(() -> new SupportRequestNotFoundException(requestId));
    }

    @Transactional(readOnly = true)
    public SupportRequest findVisibleRequest(Long requestId, Authentication authentication) {
        SupportRequest request = findById(requestId);
        User currentUser = userService.getCurrentUser(authentication);
        boolean owner = request.getOwner().getId().equals(currentUser.getId());
        boolean applicant = supportApplicationRepository.existsByApplicantIdAndSupportRequestId(currentUser.getId(), requestId);

        if (request.getStatus() != SupportRequestStatus.OPEN && !owner && !applicant) {
            throw new SupportRequestNotFoundException(requestId);
        }
        return request;
    }

    @Transactional(readOnly = true)
    public SupportRequest findOwnedRequest(Long requestId, Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        return supportRequestRepository.findByIdAndOwnerId(requestId, owner.getId())
            .orElseThrow(() -> new SupportRequestNotFoundException(requestId));
    }

    @Transactional
    public SupportRequest create(SupportRequestForm form, Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        Pet pet = petService.findOwnedPet(form.getPetId(), authentication);
        return supportRequestRepository.save(new SupportRequest(
            normalize(form.getTitle()),
            normalize(form.getDescription()),
            form.getSupportType(),
            form.getServiceDate(),
            pet,
            owner
        ));
    }

    @Transactional
    public SupportRequest update(Long requestId, SupportRequestForm form, Authentication authentication) {
        SupportRequest request = lockOwnedRequest(requestId, authentication);
        requireOpen(request);
        Pet pet = petService.findOwnedPet(form.getPetId(), authentication);
        request.setTitle(normalize(form.getTitle()));
        request.setDescription(normalize(form.getDescription()));
        request.setSupportType(form.getSupportType());
        request.setServiceDate(form.getServiceDate());
        request.setPet(pet);
        return request;
    }

    @Transactional
    public void cancel(Long requestId, Authentication authentication) {
        SupportRequest request = lockOwnedRequest(requestId, authentication);
        requireOpen(request);
        if (supportRequestRepository.transitionStatus(
                request.getId(), SupportRequestStatus.OPEN, SupportRequestStatus.CANCELLED) != 1) {
            throw new SupportRequestStateException(requestId);
        }
        entityManager.refresh(request, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        supportApplicationRepository
            .findBySupportRequestIdAndStatus(request.getId(), SupportApplicationStatus.PENDING)
            .forEach(application -> application.setStatus(SupportApplicationStatus.REJECTED));
    }

    @Transactional
    public void complete(Long requestId, Authentication authentication) {
        SupportRequest request = lockOwnedRequest(requestId, authentication);
        if (request.getStatus() != SupportRequestStatus.IN_PROGRESS) {
            throw new SupportRequestStateException(requestId);
        }
        if (supportRequestRepository.transitionStatus(
                request.getId(), SupportRequestStatus.IN_PROGRESS, SupportRequestStatus.COMPLETED) != 1) {
            throw new SupportRequestStateException(requestId);
        }
        entityManager.refresh(request, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(SupportRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        return request.getOwner().getId().equals(currentUser.getId());
    }

    public SupportRequestForm toForm(SupportRequest request) {
        SupportRequestForm form = new SupportRequestForm();
        form.setTitle(request.getTitle());
        form.setDescription(request.getDescription());
        form.setSupportType(request.getSupportType());
        form.setServiceDate(request.getServiceDate());
        form.setPetId(request.getPet().getId());
        return form;
    }

    private SupportRequest lockOwnedRequest(Long requestId, Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        SupportRequest request = supportRequestRepository.findByIdForUpdate(requestId)
            .orElseThrow(() -> new SupportRequestNotFoundException(requestId));
        entityManager.refresh(request, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!request.getOwner().getId().equals(owner.getId())) {
            throw new SupportRequestNotFoundException(requestId);
        }
        return request;
    }

    private void requireOpen(SupportRequest request) {
        if (request.getStatus() != SupportRequestStatus.OPEN) {
            throw new SupportRequestStateException(request.getId());
        }
    }

    private String normalize(String value) {
        return value.trim();
    }
}
