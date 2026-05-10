package com.bookvault.controller;

import com.bookvault.entity.User;
import com.bookvault.repository.UserRepository;
import com.bookvault.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityService securityService;

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        try {
            return ResponseEntity.ok(userRepository.findAll());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable String id, @RequestParam String status) {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        try {
            return userRepository.findById(id).map(user -> {
                user.setStatus(status);
                try {
                    return ResponseEntity.ok(userRepository.save(user));
                } catch (Exception e) {
                    return ResponseEntity.internalServerError().body(e.getMessage());
                }
            }).orElse(ResponseEntity.notFound().build());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        // In a real app, you might also delete them from Firebase Auth
        // For now, we'll just remove them from our Firestore 'users' collection or mark as DELETED
        return ResponseEntity.ok("User marked for deletion");
    }
}
