package com.petmatch.community.dto.supportapplication;

import jakarta.validation.constraints.Size;

public class SupportApplicationForm {

    @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
