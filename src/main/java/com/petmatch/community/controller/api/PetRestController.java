package com.petmatch.community.controller.api;

import static com.petmatch.community.dto.api.ApiDtoMapper.toPetForm;
import static com.petmatch.community.dto.api.ApiDtoMapper.toPetResponse;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petmatch.community.dto.api.PetApiRequest;
import com.petmatch.community.dto.api.PetApiResponse;
import com.petmatch.community.model.Pet;
import com.petmatch.community.service.PetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/pets")
public class PetRestController {

    private final PetService petService;

    public PetRestController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public List<PetApiResponse> findAll(Authentication authentication) {
        return petService.findCurrentUserPets(authentication).stream()
            .map(com.petmatch.community.dto.api.ApiDtoMapper::toPetResponse)
            .toList();
    }

    @GetMapping("/{petId}")
    public PetApiResponse findById(@PathVariable Long petId, Authentication authentication) {
        return toPetResponse(petService.findOwnedPet(petId, authentication));
    }

    @PostMapping
    public ResponseEntity<PetApiResponse> create(
        @Valid @RequestBody PetApiRequest request,
        Authentication authentication
    ) {
        Pet pet = petService.create(toPetForm(request), authentication);
        return ResponseEntity.created(URI.create("/api/v1/pets/" + pet.getId()))
            .body(toPetResponse(pet));
    }

    @PutMapping("/{petId}")
    public PetApiResponse update(
        @PathVariable Long petId,
        @Valid @RequestBody PetApiRequest request,
        Authentication authentication
    ) {
        return toPetResponse(petService.update(petId, toPetForm(request), authentication));
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> delete(@PathVariable Long petId, Authentication authentication) {
        petService.delete(petId, authentication);
        return ResponseEntity.noContent().build();
    }
}
