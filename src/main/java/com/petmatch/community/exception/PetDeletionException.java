package com.petmatch.community.exception;

public class PetDeletionException extends RuntimeException {

    public PetDeletionException(Long petId) {
        super("Pet cannot be deleted because it has support requests: " + petId);
    }
}
