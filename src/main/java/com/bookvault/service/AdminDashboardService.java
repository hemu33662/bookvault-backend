package com.bookvault.service;

import com.bookvault.dto.DashboardStatsDTO;
import com.bookvault.entity.Activity;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface AdminDashboardService {
    DashboardStatsDTO getStats() throws ExecutionException, InterruptedException;
    List<Activity> getRecentActivity() throws ExecutionException, InterruptedException;
    void logActivity(String userId, String userName, String action, String detail);
    void broadcastNotification(String message) throws ExecutionException, InterruptedException;
    List<com.bookvault.entity.Notification> getAllNotifications() throws ExecutionException, InterruptedException;
    void deleteNotification(String id);
}
