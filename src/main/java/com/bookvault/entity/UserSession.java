package com.bookvault.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private String id;
    private String userId;
    private String userEmail;
    private String deviceName; // e.g., "Chrome on Windows"
    private String location; // e.g., "Mumbai, India"
    private String lastActivity;
    private boolean isRevoked;
}
