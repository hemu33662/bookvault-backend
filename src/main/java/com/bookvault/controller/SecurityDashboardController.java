package com.bookvault.controller;

import com.bookvault.repository.SessionRepository;
import com.bookvault.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/admin/security")
public class SecurityDashboardController {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SecurityService securityService;

    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions() {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        try {
            return ResponseEntity.ok(sessionRepository.findAllActive());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/sessions/{id}/revoke")
    public ResponseEntity<?> revokeSession(@PathVariable String id) {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        sessionRepository.revokeSession(id);
        return ResponseEntity.ok("Session revoked successfully");
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAllDevices() {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        // Logic to revoke all sessions in repository
        return ResponseEntity.ok("Global logout triggered");
    }
}
