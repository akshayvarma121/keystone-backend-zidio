package com.zidio.keystone.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityUtils {

    public static KeystoneUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof KeystoneUserDetails) {
            return (KeystoneUserDetails) auth.getPrincipal();
        }
        throw new IllegalStateException("No authenticated user in context");
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    public static UUID getCurrentCustomerId() {
        return getCurrentUser().getCustomerId();
    }
}
