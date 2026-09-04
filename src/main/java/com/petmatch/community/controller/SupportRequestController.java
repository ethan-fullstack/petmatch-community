package com.petmatch.community.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.petmatch.community.dto.supportrequest.SupportRequestForm;
import com.petmatch.community.exception.PetNotFoundException;
import com.petmatch.community.exception.SupportRequestNotFoundException;
import com.petmatch.community.exception.SupportRequestStateException;
import com.petmatch.community.model.Pet;
import com.petmatch.community.model.SupportRequest;
import com.petmatch.community.model.enums.SupportRequestStatus;
import com.petmatch.community.model.enums.SupportType;
import com.petmatch.community.service.PetService;
import com.petmatch.community.service.SupportRequestService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/support-requests")
public class SupportRequestController {

    private final SupportRequestService supportRequestService;
    private final PetService petService;

    public SupportRequestController(SupportRequestService supportRequestService, PetService petService) {
        this.supportRequestService = supportRequestService;
        this.petService = petService;
    }

    @GetMapping
    public String openRequests(Model model) {
        model.addAttribute("requests", supportRequestService.findOpenRequests());
        return "support-requests/list";
    }

    @GetMapping("/mine")
    public String ownRequests(Authentication authentication, Model model) {
        model.addAttribute("requests", supportRequestService.findCurrentUserRequests(authentication));
        return "support-requests/mine";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long petId, Authentication authentication, Model model) {
        SupportRequestForm form = new SupportRequestForm();
        if (petId != null) {
            try {
                Pet pet = petService.findOwnedPet(petId, authentication);
                form.setPetId(pet.getId());
            } catch (PetNotFoundException exception) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        model.addAttribute("supportRequestForm", form);
        populateFormOptions(authentication, model);
        model.addAttribute("editing", false);
        return "support-requests/form";
    }

    @PostMapping
    public String create(@Valid SupportRequestForm supportRequestForm, BindingResult bindingResult,
        Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormOptions(authentication, model);
            model.addAttribute("editing", false);
            return "support-requests/form";
        }
        try {
            SupportRequest request = supportRequestService.create(supportRequestForm, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitud de apoyo publicada correctamente.");
            return "redirect:/support-requests/" + request.getId();
        } catch (PetNotFoundException exception) {
            bindingResult.rejectValue("petId", "pet.invalid", "Selecciona una mascota que te pertenezca.");
            populateFormOptions(authentication, model);
            model.addAttribute("editing", false);
            return "support-requests/form";
        }
    }

    @GetMapping("/{requestId}")
    public String detail(@PathVariable Long requestId, Authentication authentication, Model model) {
        try {
            SupportRequest request = supportRequestService.findVisibleRequest(requestId, authentication);
            model.addAttribute("request", request);
            model.addAttribute("ownerView", supportRequestService.isOwner(request, authentication));
            return "support-requests/detail";
        } catch (SupportRequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{requestId}/edit")
    public String editForm(@PathVariable Long requestId, Authentication authentication, Model model,
        RedirectAttributes redirectAttributes) {
        SupportRequest request = findOwnedRequest(requestId, authentication);
        if (request.getStatus() != SupportRequestStatus.OPEN) {
            redirectAttributes.addFlashAttribute("errorMessage", "Solo las solicitudes abiertas pueden editarse.");
            return "redirect:/support-requests/" + requestId;
        }
        model.addAttribute("request", request);
        model.addAttribute("supportRequestForm", supportRequestService.toForm(request));
        populateFormOptions(authentication, model);
        model.addAttribute("editing", true);
        return "support-requests/form";
    }

    @PostMapping("/{requestId}")
    public String update(@PathVariable Long requestId, @Valid SupportRequestForm supportRequestForm,
        BindingResult bindingResult, Authentication authentication, Model model,
        RedirectAttributes redirectAttributes) {
        SupportRequest request = findOwnedRequest(requestId, authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("request", request);
            populateFormOptions(authentication, model);
            model.addAttribute("editing", true);
            return "support-requests/form";
        }
        try {
            supportRequestService.update(requestId, supportRequestForm, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitud actualizada correctamente.");
            return "redirect:/support-requests/" + requestId;
        } catch (PetNotFoundException exception) {
            bindingResult.rejectValue("petId", "pet.invalid", "Selecciona una mascota que te pertenezca.");
            model.addAttribute("request", request);
            populateFormOptions(authentication, model);
            model.addAttribute("editing", true);
            return "support-requests/form";
        } catch (SupportRequestStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "La solicitud ya no está abierta y no puede editarse.");
            return "redirect:/support-requests/" + requestId;
        }
    }

    @PostMapping("/{requestId}/cancel")
    public String cancel(@PathVariable Long requestId, Authentication authentication,
        RedirectAttributes redirectAttributes) {
        try {
            supportRequestService.cancel(requestId, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitud cancelada correctamente.");
        } catch (SupportRequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (SupportRequestStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Solo una solicitud abierta puede cancelarse.");
        }
        return "redirect:/support-requests/" + requestId;
    }

    @PostMapping("/{requestId}/complete")
    public String complete(@PathVariable Long requestId, Authentication authentication,
        RedirectAttributes redirectAttributes) {
        try {
            supportRequestService.complete(requestId, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Solicitud completada correctamente.");
        } catch (SupportRequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (SupportRequestStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Solo una solicitud en progreso puede completarse.");
        }
        return "redirect:/support-requests/" + requestId;
    }

    private SupportRequest findOwnedRequest(Long requestId, Authentication authentication) {
        try {
            return supportRequestService.findOwnedRequest(requestId, authentication);
        } catch (SupportRequestNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private void populateFormOptions(Authentication authentication, Model model) {
        List<Pet> pets = petService.findCurrentUserPets(authentication);
        model.addAttribute("pets", pets);
        model.addAttribute("supportTypes", SupportType.values());
    }
}
