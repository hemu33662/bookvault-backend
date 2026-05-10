package com.bookvault.controller;

import com.bookvault.service.DatabaseMaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/db")
public class DatabaseController {

    @Autowired
    private DatabaseMaintenanceService databaseMaintenanceService;

    @PostMapping("/setup")
    public ResponseEntity<?> setupDatabase() {
        try {
            databaseMaintenanceService.initializeDatabase();
            return ResponseEntity.ok(Map.of("message", "Database collections recreated successfully."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
