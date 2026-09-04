package com.petmatch.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.petmatch.community.dto.auth.RegistrationForm;
import com.petmatch.community.exception.DuplicateEmailException;
import com.petmatch.community.service.UserService;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute("registrationForm") RegistrationForm form,
        BindingResult bindingResult
    ) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Las contraseñas no coinciden");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(form);
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "email.duplicate", "Ya existe una cuenta con este correo");
            return "auth/register";
        }

        return "redirect:/login?registered";
    }
}
