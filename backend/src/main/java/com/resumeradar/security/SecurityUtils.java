package com.resumeradar.security;

import com.resumeradar.entity.User;
import com.resumeradar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.ofNullable(userDetails.getUser());
        }
        return userRepository.findByEmail(auth.getName());
    }

    public User getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Authentication required: no active session found"));
    }
}
