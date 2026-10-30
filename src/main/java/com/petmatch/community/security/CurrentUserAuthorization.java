package com.petmatch.community.security;

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
 * Recheck the persisted active flag for every protected web request.
 * An already authenticated session must not outlive account deactivation.
 */
@Component
public class CurrentUserAuthorization implements AuthorizationManager<RequestAuthorizationContext> {

    private final UserRepository users;

    public CurrentUserAuthorization(UserRepository users) {
        this.users = users;
    }

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication current = authentication.get();
        if (current == null || !current.isAuthenticated()
                || current instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }

        boolean active = users.findByEmailIgnoreCase(current.getName())
                .map(user -> user.isActive())
                .orElse(false);
        return new AuthorizationDecision(active);
    }
}
