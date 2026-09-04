package com.petmatch.community.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.petmatch.community.dto.supportapplication.SupportApplicationForm;
import com.petmatch.community.exception.SupportApplicationRuleException;
import com.petmatch.community.model.SupportApplication;
import com.petmatch.community.model.SupportRequest;
import com.petmatch.community.model.User;
import com.petmatch.community.model.enums.SupportApplicationStatus;
import com.petmatch.community.model.enums.SupportRequestStatus;
import com.petmatch.community.repository.SupportApplicationRepository;
import com.petmatch.community.repository.SupportRequestRepository;

@ExtendWith(MockitoExtension.class)
class SupportApplicationServiceTests {

    @Mock
    private SupportApplicationRepository supportApplicationRepository;

    @Mock
    private SupportRequestRepository supportRequestRepository;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SupportApplicationService supportApplicationService;

    @Test
    void acceptMovesRequestToInProgressAndRejectsOtherPendingApplications() {
        User owner = new User("Owner", "owner@example.com", "hash");
        SupportRequest request = org.mockito.Mockito.mock(SupportRequest.class);
        SupportApplication selected = org.mockito.Mockito.mock(SupportApplication.class);
        SupportApplication otherPending = org.mockito.Mockito.mock(SupportApplication.class);

        when(userService.getCurrentUser(authentication)).thenReturn(owner);
        when(supportApplicationRepository.findByIdAndSupportRequestOwnerId(20L, owner.getId()))
            .thenReturn(Optional.of(selected));
        when(selected.getSupportRequest()).thenReturn(request);
        when(selected.getStatus()).thenReturn(SupportApplicationStatus.PENDING);
        when(selected.getId()).thenReturn(20L);
        when(request.getId()).thenReturn(10L);
        when(request.getStatus()).thenReturn(SupportRequestStatus.OPEN);
        when(supportRequestRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(request));
        when(supportApplicationRepository.countBySupportRequestIdAndStatus(
            10L,
            SupportApplicationStatus.ACCEPTED
        )).thenReturn(0L);
        when(supportApplicationRepository.findBySupportRequestIdAndStatus(
            10L,
            SupportApplicationStatus.PENDING
        )).thenReturn(List.of(selected, otherPending));
        when(otherPending.getId()).thenReturn(21L);

        supportApplicationService.accept(20L, authentication);

        verify(selected).setStatus(SupportApplicationStatus.ACCEPTED);
        verify(request).setStatus(SupportRequestStatus.IN_PROGRESS);
        verify(otherPending).setStatus(SupportApplicationStatus.REJECTED);
    }

    @Test
    void applyRejectsExpiredOpenRequest() {
        User applicant = org.mockito.Mockito.mock(User.class);
        SupportRequest request = org.mockito.Mockito.mock(SupportRequest.class);
        SupportApplicationForm form = new SupportApplicationForm();

        when(userService.getCurrentUser(authentication)).thenReturn(applicant);
        when(supportRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(request.getStatus()).thenReturn(SupportRequestStatus.OPEN);
        when(request.getServiceDate()).thenReturn(LocalDateTime.now().minusMinutes(1));

        assertThrows(
            SupportApplicationRuleException.class,
            () -> supportApplicationService.apply(10L, form, authentication)
        );
    }
}
