package com.petmatch.community.controller.api;

import static com.petmatch.community.dto.api.ApiDtoMapper.toSupportApplicationForm;
import static com.petmatch.community.dto.api.ApiDtoMapper.toSupportApplicationResponse;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petmatch.community.dto.api.SupportApplicationApiRequest;
import com.petmatch.community.dto.api.SupportApplicationApiResponse;
import com.petmatch.community.model.SupportApplication;
import com.petmatch.community.service.SupportApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class SupportApplicationRestController {

    private final SupportApplicationService supportApplicationService;

    public SupportApplicationRestController(SupportApplicationService supportApplicationService) {
        this.supportApplicationService = supportApplicationService;
    }

    @GetMapping("/support-applications/mine")
    public List<SupportApplicationApiResponse> findMine(Authentication authentication) {
        return supportApplicationService.findCurrentUserApplications(authentication).stream()
            .map(com.petmatch.community.dto.api.ApiDtoMapper::toSupportApplicationResponse)
            .toList();
    }

    @PostMapping("/support-requests/{requestId}/applications")
    public ResponseEntity<SupportApplicationApiResponse> apply(
        @PathVariable Long requestId,
        @Valid @RequestBody SupportApplicationApiRequest request,
        Authentication authentication
    ) {
        SupportApplication created = supportApplicationService.apply(
            requestId,
            toSupportApplicationForm(request),
            authentication
        );
        return ResponseEntity.created(URI.create("/api/v1/support-applications/" + created.getId()))
            .body(toSupportApplicationResponse(created));
    }

    @GetMapping("/support-requests/{requestId}/applications")
    public List<SupportApplicationApiResponse> findReceived(
        @PathVariable Long requestId,
        Authentication authentication
    ) {
        return supportApplicationService.findReceivedApplications(requestId, authentication).stream()
            .map(com.petmatch.community.dto.api.ApiDtoMapper::toSupportApplicationResponse)
            .toList();
    }

    @PostMapping("/support-applications/{applicationId}/accept")
    public ResponseEntity<Void> accept(
        @PathVariable Long applicationId,
        Authentication authentication
    ) {
        supportApplicationService.accept(applicationId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/support-applications/{applicationId}/reject")
    public ResponseEntity<Void> reject(
        @PathVariable Long applicationId,
        Authentication authentication
    ) {
        supportApplicationService.reject(applicationId, authentication);
        return ResponseEntity.noContent().build();
    }
}
