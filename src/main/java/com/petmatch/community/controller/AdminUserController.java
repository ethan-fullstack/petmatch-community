package com.petmatch.community.controller;

import com.petmatch.community.model.enums.Role;
import com.petmatch.community.service.AdminUserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {
    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("users", service.list(page));
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/{id}/role")
    public String change(
            @PathVariable Long id,
            @RequestParam Role role,
            Principal actor,
            RedirectAttributes flash) {
        try {
            service.changeRole(actor.getName(), id, role);
            flash.addFlashAttribute(
                    "successMessage",
                    "Rol actualizado. El usuario debe iniciar sesión nuevamente.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
