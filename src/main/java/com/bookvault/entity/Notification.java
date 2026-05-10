package com.bookvault.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private String message;
    private String type; // e.g., INFO, WARNING, BROADCAST
    private String createdAt;
    private boolean active;
}
