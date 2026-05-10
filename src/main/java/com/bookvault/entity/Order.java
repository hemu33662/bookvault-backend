package com.bookvault.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String id;
    private String referenceId; // Secure unique tracking ID
    private String userId;
    private String bookId;
    private String bookTitle;
    private Double amount;
    private String customerName;
    private String whatsappNumber;
    private String type; // EBOOK_REQUEST, PAPERBACK_ORDER
    private String status; // CREATED, APPROVED, SHIPPED, REJECTED
    private String shippingAddress;
    private String message;
    private String createdAt;
    private String updatedAt;

}
