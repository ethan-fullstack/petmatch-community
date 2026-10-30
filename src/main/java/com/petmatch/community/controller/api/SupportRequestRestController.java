package com.petmatch.community.controller.api;

import static com.petmatch.community.dto.api.ApiDtoMapper.toSupportRequestForm;
import static com.petmatch.community.dto.api.ApiDtoMapper.toSupportRequestResponse;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petmatch.community.dto.api.SupportRequestApiRequest;
import com.petmatch.community.dto.api.SupportRequestApiResponse;
import com.petmatch.community.model.SupportRequest;
import com.petmatch.community.service.SupportRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/support-requests")
public class SupportRequestRestController {

    private final SupportRequestService supportRequestService;

    public SupportRequestRestController(SupportRequestService supportRequestService) {
        this.supportRequestService = supportRequestService;
    }

    @GetMapping
    public List<SupportRequestApiResponse> findOpenRequests() {
        return supportRequestService.findOpenRequests().stream()
            .map(com.petmatch.community.dto.api.ApiDtoMapper::toSupportRequestResponse)
            .toList();
    }

    @GetMapping("/mine")
    public List<SupportRequestApiResponse> findMine(Authentication authentication) {
        return supportRequestService.findCurrentUserRequests(authentication).stream()
            .map(com.petmatch.community.dto.api.ApiDtoMapper::toSupportRequestResponse)
            .toList();
    }

    @GetMapping("/{requestId}")
    public SupportRequestApiResponse findById(
        @PathVariable Long requestId,
        Authentication authentication
    ) {
        return toSupportRequestResponse(supportRequestService.findVisibleRequest(requestId, authentication));
    }

    @PostMapping
    public ResponseEntity<SupportRequestApiResponse> create(
        @Valid @RequestBody SupportRequestApiRequest request,
        Authentication authentication
    ) {
        SupportRequest created = supportRequestService.create(toSupportRequestForm(request), authentication);
        return ResponseEntity.created(URI.create("/api/v1/support-requests/" + created.getId()))
            .body(toSupportRequestResponse(created));
    }

    @PutMapping("/{requestId}")
    public SupportRequestApiResponse update(
        @PathVariable Long requestId,
        @Valid @RequestBody SupportRequestApiRequest request,
        Authentication authentication
    ) {
        return toSupportRequestResponse(
            supportRequestService.update(requestId, toSupportRequestForm(request), authentication)
        );
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long requestId, Authentication authentication) {
        supportRequestService.cancel(requestId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{requestId}/complete")
    public ResponseEntity<Void> complete(@PathVariable Long requestId, Authentication authentication) {
        supportRequestService.complete(requestId, authentication);
        return ResponseEntity.noContent().build();
    }
}
