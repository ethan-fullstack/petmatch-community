package com.petmatch.community.model;

import com.petmatch.community.model.enums.Role;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_changes")
public class RoleChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role previousRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role newRole;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    protected RoleChange() {}

    public RoleChange(Long actorId, Long targetId, Role previousRole, Role newRole) {
        this.actorId = actorId;
        this.targetId = targetId;
        this.previousRole = previousRole;
        this.newRole = newRole;
        this.changedAt = LocalDateTime.now();
    }

    public Long getActorId() {
        return actorId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Role getPreviousRole() {
        return previousRole;
    }

    public Role getNewRole() {
        return newRole;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
