package com.petmatch.community.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petmatch.community.dto.pet.PetForm;
import com.petmatch.community.exception.PetDeletionException;
import com.petmatch.community.exception.PetNotFoundException;
import com.petmatch.community.model.Pet;
import com.petmatch.community.model.User;
import com.petmatch.community.repository.PetRepository;
import com.petmatch.community.repository.SupportRequestRepository;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final UserService userService;

    public PetService(
        PetRepository petRepository,
        SupportRequestRepository supportRequestRepository,
        UserService userService
    ) {
        this.petRepository = petRepository;
        this.supportRequestRepository = supportRequestRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Pet> findCurrentUserPets(Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        return petRepository.findByOwnerIdOrderByNameAsc(owner.getId());
    }

    @Transactional(readOnly = true)
    public Pet findOwnedPet(Long petId, Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        return petRepository.findByIdAndOwnerId(petId, owner.getId())
            .orElseThrow(() -> new PetNotFoundException(petId));
    }

    @Transactional
    public Pet create(PetForm form, Authentication authentication) {
        User owner = userService.getCurrentUser(authentication);
        Pet pet = new Pet(
            normalize(form.getName()),
            normalize(form.getSpecies()),
            form.getAge(),
            normalizeNullable(form.getDescription()),
            owner
        );
        return petRepository.save(pet);
    }

    @Transactional
    public Pet update(Long petId, PetForm form, Authentication authentication) {
        Pet pet = findOwnedPet(petId, authentication);
        pet.setName(normalize(form.getName()));
        pet.setSpecies(normalize(form.getSpecies()));
        pet.setAge(form.getAge());
        pet.setDescription(normalizeNullable(form.getDescription()));
        return pet;
    }

    @Transactional
    public void delete(Long petId, Authentication authentication) {
        Pet pet = findOwnedPet(petId, authentication);
        if (supportRequestRepository.existsByPetId(pet.getId())) {
            throw new PetDeletionException(pet.getId());
        }
        petRepository.delete(pet);
    }

    public PetForm toForm(Pet pet) {
        PetForm form = new PetForm();
        form.setName(pet.getName());
        form.setSpecies(pet.getSpecies());
        form.setAge(pet.getAge());
        form.setDescription(pet.getDescription());
        return form;
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
