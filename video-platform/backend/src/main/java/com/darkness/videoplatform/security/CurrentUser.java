package com.darkness.videoplatform.security;

import com.darkness.videoplatform.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    /**
     * Returns the currently authenticated user, or null if not authenticated.
     */
    public User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Returns the currently authenticated user, or throws if not authenticated.
     */
    public User require() {
        User user = get();
        if (user == null) {
            throw new com.darkness.videoplatform.exception.UnauthorizedException("Authentication required");
        }
        return user;
    }
}
