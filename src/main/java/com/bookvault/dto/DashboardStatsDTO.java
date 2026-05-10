package com.bookvault.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class DashboardStatsDTO {
    // User Metrics
    private long totalUsers;
    private long activeUsersToday;
    private long blockedUsers;

    // Book Metrics
    private long totalBooks;
    private long publishedBooks;
    private long draftBooks;
    private Map<String, Long> booksByType; // e.g., "FREE": 10, "PAID_EBOOK": 20

    // Order Metrics
    private long totalOrders;
    private long pendingOrders;
    private long approvedOrders;
    private long rejectedOrders;
    private double totalRevenue;

    // Engagement
    private long totalComments;
}
