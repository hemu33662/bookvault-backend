package com.bookvault.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String id; // Document ID in Firestore
    private String firebaseUid;
    private String email;
    private Role role;
    private String status; // ACTIVE, BLOCKED
    private String createdAt; // Stored as ISO String in Firestore
    private int activeDevicesCount;

}
