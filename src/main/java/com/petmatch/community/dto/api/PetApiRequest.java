package com.petmatch.community.dto.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PetApiRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String name,

    @NotBlank(message = "La especie es obligatoria")
    @Size(max = 80, message = "La especie no puede superar los 80 caracteres")
    String species,

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 0, message = "La edad no puede ser negativa")
    Integer age,

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    String description
) {
}
