package com.bookvault.service;

import com.bookvault.entity.Role;
import com.bookvault.entity.User;
import com.bookvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
public class SecurityService {

    @Autowired
    private UserRepository userRepository;

    @Value("${ADMIN_EMAIL:hemanth.nitm@gmail.com}")
    private String masterAdminEmail;

    public boolean isAdmin() {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) return false;
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                String email = jwt.getClaimAsString("email");
                
                // 1. Super Admin Bypass (via email)
                if (masterAdminEmail != null && masterAdminEmail.equalsIgnoreCase(email)) {
                    return true;
                }
                
                // 2. Standard Database Check
                String firebaseUid = jwt.getSubject();
                Optional<User> user = userRepository.findByFirebaseUid(firebaseUid);
                return user.isPresent() && user.get().getRole() == Role.ADMIN;
            }
        } catch (Exception e) {
            System.err.println("Admin check failed: " + e.getMessage());
        }
        return false;
    }

    public String getCurrentUserFirebaseUid() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt) {
            return ((Jwt) principal).getSubject();
        }
        return null;
    }

    public String getCurrentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt) {
            return ((Jwt) principal).getClaimAsString("email");
        }
        return null;
    }

    public boolean isUser() {
        return getCurrentUserFirebaseUid() != null;
    }
}
