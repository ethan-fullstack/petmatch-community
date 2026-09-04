package com.petmatch.community.dto.api;

import jakarta.validation.constraints.Size;

public record SupportApplicationApiRequest(
    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    String message
) {
}
