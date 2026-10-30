package com.petmatch.community.dto.api;

public record PetApiResponse(
    Long id,
    String name,
    String species,
    Integer age,
    String description
) {
}
