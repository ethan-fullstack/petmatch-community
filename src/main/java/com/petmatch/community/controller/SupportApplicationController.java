package com.petmatch.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.petmatch.community.dto.supportapplication.SupportApplicationForm;
import com.petmatch.community.exception.SupportApplicationNotFoundException;
import com.petmatch.community.exception.SupportApplicationRuleException;
import com.petmatch.community.exception.SupportApplicationStateException;
import com.petmatch.community.exception.SupportRequestNotFoundException;
import com.petmatch.community.model.SupportRequest;
import com.petmatch.community.service.SupportApplicationService;
import com.petmatch.community.service.SupportRequestService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/support-applications")
public class SupportApplicationController {

    private final SupportApplicationService supportApplicationService;
    private final SupportRequestService supportRequestService;

    public SupportApplicationController(
        SupportApplicationService supportApplicationService,
        SupportRequestService supportRequestService
    ) {
        this.supportApplicationService = supportApplicationService;
        this.supportRequestService = supportRequestService;
    }

    @GetMapping("/mine")
    public String mine(Authentication authentication, Model model) {
        model.addAttribute("applications", supportApplicationService.findCurrentUserApplications(authentication));
        return "support-applications/mine";
    }

    @GetMapping("/request/{requestId}/new")
    public String applyForm(@PathVariable Long requestId, Authentication authentication, Model model) {
        try {
            SupportRequest request = supportRequestService.findVisibleRequest(requestId, authentication);
            if (supportRequestService.isOwner(request, authentication)) {
                return "redirect:/support-requests/" + requestId;
            }
            model.addAttribute("request", request);
            model.addAttribute("supportApplicationForm", new SupportApplicationForm());
            return "support-applications/form";
        } catch (SupportRequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/request/{requestId}")
    public String apply(
        @PathVariable Long requestId,
        @Valid SupportApplicationForm supportApplicationForm,
        BindingResult bindingResult,
        Authentication authentication,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("request", supportRequestService.findVisibleRequest(requestId, authentication));
            return "support-applications/form";
        }

        try {
            supportApplicationService.apply(requestId, supportApplicationForm, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Postulación enviada correctamente.");
            return "redirect:/support-applications/mine";
        } catch (SupportApplicationRuleException exception) {
            bindingResult.reject("application.rule", exception.getMessage());
            model.addAttribute("request", supportRequestService.findVisibleRequest(requestId, authentication));
            return "support-applications/form";
        }
    }

    @GetMapping("/request/{requestId}")
    public String received(@PathVariable Long requestId, Authentication authentication, Model model) {
        try {
            model.addAttribute("request", supportRequestService.findOwnedRequest(requestId, authentication));
            model.addAttribute("applications", supportApplicationService.findReceivedApplications(requestId, authentication));
            return "support-applications/received";
        } catch (SupportRequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{applicationId}/accept")
    public String accept(
        @PathVariable Long applicationId,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        try {
            supportApplicationService.accept(applicationId, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Postulación aceptada. La solicitud está en progreso.");
        } catch (SupportApplicationNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (SupportApplicationStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Esta postulación ya no puede aceptarse.");
        }
        return "redirect:/support-requests/mine";
    }

    @PostMapping("/{applicationId}/reject")
    public String reject(
        @PathVariable Long applicationId,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        try {
            supportApplicationService.reject(applicationId, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Postulación rechazada.");
        } catch (SupportApplicationNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (SupportApplicationStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Esta postulación ya no puede rechazarse.");
        }
        return "redirect:/support-requests/mine";
    }
}
