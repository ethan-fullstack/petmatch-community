package com.petmatch.community.service;

import com.petmatch.community.model.*;
import com.petmatch.community.model.enums.Role;
import com.petmatch.community.repository.*;

import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    public record UserRow(Long id, String name, String email, Role role, boolean active) {}

    private final UserRepository users;
    private final RoleChangeRepository changes;

    public AdminUserService(UserRepository users, RoleChangeRepository changes) {
        this.users = users;
        this.changes = changes;
    }

    @Transactional(readOnly = true)
    public Page<UserRow> list(int page) {
        return users.findAll(PageRequest.of(Math.max(0, page), 20, Sort.by("id")))
                .map(
                        u ->
                                new UserRow(
                                        u.getId(),
                                        u.getName(),
                                        u.getEmail(),
                                        u.getRole(),
                                        u.isActive()));
    }

    @Transactional
    public void changeRole(String actorEmail, Long targetId, Role role) {
        if (targetId == null || targetId < 1 || role == null)
            throw new IllegalArgumentException("Usuario o rol inválido");
        var locked = users.lockForRoleChange();
        var actor =
                locked.stream()
                        .filter(u -> u.getEmail().equalsIgnoreCase(actorEmail))
                        .findFirst()
                        .orElseThrow(
                                () -> new AccessDeniedException("Administrador no disponible"));
        if (!actor.isActive() || actor.getRole() != Role.ADMIN)
            throw new AccessDeniedException(
                    "Se requiere un administrador activo; inicia sesión nuevamente");
        var target =
                locked.stream()
                        .filter(u -> u.getId().equals(targetId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (target.getRole() == role) return;
        if (target.isActive()
                && target.getRole() == Role.ADMIN
                && role != Role.ADMIN
                && locked.stream().filter(u -> u.isActive() && u.getRole() == Role.ADMIN).count()
                        <= 1)
            throw new IllegalArgumentException(
                    "No se puede degradar al último administrador activo");
        changes.save(new RoleChange(actor.getId(), targetId, target.getRole(), role));
        target.setRole(role);
    }
}
