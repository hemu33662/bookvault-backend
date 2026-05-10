package com.bookvault.controller;

import com.bookvault.dto.DashboardStatsDTO;
import com.bookvault.entity.Activity;
import com.bookvault.service.AdminDashboardService;
import com.bookvault.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService dashboardService;

    @Autowired
    private SecurityService securityService;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only administrators can access dashboard analytics");
        }
        try {
            return ResponseEntity.ok(dashboardService.getStats());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/activity")
    public ResponseEntity<?> getRecentActivity() {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only administrators can access the activity feed");
        }
        try {
            return ResponseEntity.ok(dashboardService.getRecentActivity());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcast(@RequestBody Map<String, String> payload) {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only administrators can broadcast notifications");
        }
        try {
            String message = payload.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Message content is required");
            }
            dashboardService.broadcastNotification(message);
            return ResponseEntity.ok(Map.of("message", "Notification broadcasted successfully"));
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getAllNotifications() {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        try {
            return ResponseEntity.ok(dashboardService.getAllNotifications());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable String id) {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        dashboardService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
    }
}
