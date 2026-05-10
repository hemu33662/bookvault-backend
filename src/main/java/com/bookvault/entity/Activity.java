package com.bookvault.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    private String id;
    private String userId;
    private String userName;
    private String action; // e.g., "PURCHASED_BOOK", "POSTED_COMMENT", "LOGGED_IN"
    private String detail; // e.g., "The Silent Night"
    private String timestamp;
}
