package com.bookvault.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAccess {

    private String id;
    private String userId; // Reference to User doc ID
    private String bookId; // Reference to Book doc ID
    private String accessType; // FULL, PREVIEW, EXPIRED
    private String status; // ACTIVE, REVOKED
    private String expiryDate;
    private String grantedAt;

}
