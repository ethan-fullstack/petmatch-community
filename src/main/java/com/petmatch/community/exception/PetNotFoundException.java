package com.petmatch.community.exception;

public class PetNotFoundException extends RuntimeException {

    public PetNotFoundException(Long petId) {
        super("Pet not found: " + petId);
    }
}
