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

import com.petmatch.community.dto.pet.PetForm;
import com.petmatch.community.exception.PetDeletionException;
import com.petmatch.community.exception.PetNotFoundException;
import com.petmatch.community.model.Pet;
import com.petmatch.community.service.PetService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("pets", petService.findCurrentUserPets(authentication));
        return "pets/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("petForm", new PetForm());
        model.addAttribute("editing", false);
        return "pets/form";
    }

    @PostMapping
    public String create(
        @Valid PetForm petForm,
        BindingResult bindingResult,
        Authentication authentication,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            return "pets/form";
        }

        Pet pet = petService.create(petForm, authentication);
        redirectAttributes.addFlashAttribute("successMessage", "Mascota registrada correctamente.");
        return "redirect:/pets/" + pet.getId();
    }

    @GetMapping("/{petId}")
    public String detail(@PathVariable Long petId, Authentication authentication, Model model) {
        model.addAttribute("pet", findOwnedPet(petId, authentication));
        return "pets/detail";
    }

    @GetMapping("/{petId}/edit")
    public String editForm(@PathVariable Long petId, Authentication authentication, Model model) {
        Pet pet = findOwnedPet(petId, authentication);
        model.addAttribute("pet", pet);
        model.addAttribute("petForm", petService.toForm(pet));
        model.addAttribute("editing", true);
        return "pets/form";
    }

    @PostMapping("/{petId}")
    public String update(
        @PathVariable Long petId,
        @Valid PetForm petForm,
        BindingResult bindingResult,
        Authentication authentication,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        Pet pet = findOwnedPet(petId, authentication);

        if (bindingResult.hasErrors()) {
            model.addAttribute("pet", pet);
            model.addAttribute("editing", true);
            return "pets/form";
        }

        petService.update(petId, petForm, authentication);
        redirectAttributes.addFlashAttribute("successMessage", "Mascota actualizada correctamente.");
        return "redirect:/pets/" + petId;
    }

    @PostMapping("/{petId}/delete")
    public String delete(
        @PathVariable Long petId,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        try {
            petService.delete(petId, authentication);
            redirectAttributes.addFlashAttribute("successMessage", "Mascota eliminada correctamente.");
        } catch (PetDeletionException exception) {
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "No puedes eliminar esta mascota porque tiene solicitudes de apoyo asociadas."
            );
        } catch (PetNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return "redirect:/pets";
    }

    private Pet findOwnedPet(Long petId, Authentication authentication) {
        try {
            return petService.findOwnedPet(petId, authentication);
        } catch (PetNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
