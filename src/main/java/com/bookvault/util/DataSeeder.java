package com.bookvault.util;

import com.bookvault.service.DatabaseMaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private DatabaseMaintenanceService databaseMaintenanceService;

    @Override
    public void run(String... args) throws Exception {
        try {
            databaseMaintenanceService.initializeDatabase();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }
}
