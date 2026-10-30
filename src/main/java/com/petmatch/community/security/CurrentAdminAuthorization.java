package com.petmatch.community.security;

import com.petmatch.community.model.enums.Role;
import com.petmatch.community.repository.UserRepository;
import java.util.function.Supplier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * Enforce the persisted ADMIN role on every administrative HTTP request.
 * A demoted or disabled account cannot keep using privileges cached in its session.
 * A promotion still requires a new login to acquire ROLE_ADMIN in the session.
 */
@Component
public class CurrentAdminAuthorization implements AuthorizationManager<RequestAuthorizationContext> {
    private final UserRepository users;

    public CurrentAdminAuthorization(UserRepository users) {
        this.users = users;
    }

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication current = authentication.get();
        if (current == null || !current.isAuthenticated()
                || current instanceof AnonymousAuthenticationToken
                || current.getAuthorities().stream()
                        .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return new AuthorizationDecision(false);
        }
        boolean permitted = users.findByEmailIgnoreCase(current.getName())
                .map(user -> user.isActive() && user.getRole() == Role.ADMIN)
                .orElse(false);
        return new AuthorizationDecision(permitted);
    }
}
