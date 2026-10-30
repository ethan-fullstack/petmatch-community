package com.petmatch.community.dto.api;

import java.time.LocalDateTime;

import com.petmatch.community.model.enums.SupportType;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupportRequestApiRequest(
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    String title,

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    String description,

    @NotNull(message = "Selecciona un tipo de apoyo")
    SupportType supportType,

    @NotNull(message = "La fecha del servicio es obligatoria")
    @Future(message = "La fecha del servicio debe estar en el futuro")
    LocalDateTime serviceDate,

    @NotNull(message = "Selecciona una mascota")
    Long petId
) {
}
