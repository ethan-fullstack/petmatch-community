package com.petmatch.community.service;

import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petmatch.community.dto.auth.RegistrationForm;
import com.petmatch.community.exception.DuplicateEmailException;
import com.petmatch.community.model.User;
import com.petmatch.community.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegistrationForm form) {
        String normalizedEmail = normalizeEmail(form.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        User user = new User(
            form.getName().trim(),
            normalizedEmail,
            passwordEncoder.encode(form.getPassword())
        );

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("No authenticated user");
        }

        return findByEmail(authentication.getName());
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
