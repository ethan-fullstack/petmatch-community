package com.petmatch.community.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.petmatch.community.model.SupportApplication;
import com.petmatch.community.model.SupportRequest;
import com.petmatch.community.model.User;
import com.petmatch.community.model.enums.SupportApplicationStatus;
import com.petmatch.community.model.enums.SupportRequestStatus;
import com.petmatch.community.repository.SupportApplicationRepository;
import com.petmatch.community.repository.SupportRequestRepository;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTests {

    @Mock
    private SupportRequestRepository supportRequestRepository;

    @Mock
    private SupportApplicationRepository supportApplicationRepository;

    @Mock
    private PetService petService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SupportRequestService supportRequestService;

    @Test
    void cancelRejectsPendingApplications() {
        User owner = new User("Owner", "owner@example.com", "hash");
        SupportRequest request = org.mockito.Mockito.mock(SupportRequest.class);
        SupportApplication pendingApplication = org.mockito.Mockito.mock(SupportApplication.class);

        when(userService.getCurrentUser(authentication)).thenReturn(owner);
        when(supportRequestRepository.findByIdAndOwnerId(10L, owner.getId()))
            .thenReturn(Optional.of(request));
        when(request.getStatus()).thenReturn(SupportRequestStatus.OPEN);
        when(request.getId()).thenReturn(10L);
        when(supportApplicationRepository.findBySupportRequestIdAndStatus(
            10L,
            SupportApplicationStatus.PENDING
        )).thenReturn(List.of(pendingApplication));

        supportRequestService.cancel(10L, authentication);

        verify(request).setStatus(SupportRequestStatus.CANCELLED);
        verify(pendingApplication).setStatus(SupportApplicationStatus.REJECTED);
    }
}
