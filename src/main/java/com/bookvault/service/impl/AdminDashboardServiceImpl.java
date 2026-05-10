package com.bookvault.service.impl;

import com.bookvault.dto.DashboardStatsDTO;
import com.bookvault.entity.Activity;
import com.bookvault.entity.Book;
import com.bookvault.entity.Order;
import com.bookvault.entity.User;
import com.bookvault.repository.ActivityRepository;
import com.bookvault.repository.BookRepository;
import com.bookvault.repository.NotificationRepository;
import com.bookvault.repository.OrderRepository;
import com.bookvault.repository.UserRepository;
import com.bookvault.entity.Notification;
import com.bookvault.service.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public DashboardStatsDTO getStats() throws ExecutionException, InterruptedException {
        List<User> users = userRepository.findAll();
        List<Book> books = bookRepository.findAll();
        List<Order> orders = orderRepository.findAll();

        Map<String, Long> booksByType = books.stream()
                .collect(Collectors.groupingBy(b -> b.getType().name(), Collectors.counting()));

        double totalRevenue = orders.stream()
                .filter(o -> "APPROVED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus()))
                .mapToDouble(o -> 0.0) // In a real app, you'd calculate this from the book price at the time of order
                .sum();

        return DashboardStatsDTO.builder()
                .totalUsers(users.size())
                .activeUsersToday(users.stream().filter(u -> "ACTIVE".equals(u.getStatus())).count())
                .blockedUsers(users.stream().filter(u -> "BLOCKED".equals(u.getStatus())).count())
                .totalBooks(books.size())
                .publishedBooks(books.stream().filter(b -> "PUBLISHED".equals(b.getStatus())).count())
                .draftBooks(books.stream().filter(b -> "DRAFT".equals(b.getStatus())).count())
                .booksByType(booksByType)
                .totalOrders(orders.size())
                .pendingOrders(orders.stream().filter(o -> "CREATED".equals(o.getStatus())).count())
                .approvedOrders(orders.stream().filter(o -> "APPROVED".equals(o.getStatus())).count())
                .rejectedOrders(orders.stream().filter(o -> "REJECTED".equals(o.getStatus())).count())
                .totalRevenue(totalRevenue)
                .totalComments(books.stream().mapToLong(b -> b.getComments() != null ? b.getComments().size() : 0).sum())
                .build();
    }

    @Override
    public List<Activity> getRecentActivity() throws ExecutionException, InterruptedException {
        return activityRepository.findRecent(20);
    }

    @Override
    public void logActivity(String userId, String userName, String action, String detail) {
        Activity activity = Activity.builder()
                .userId(userId)
                .userName(userName)
                .action(action)
                .detail(detail)
                .timestamp(LocalDateTime.now().toString())
                .build();
        activityRepository.save(activity);
    }

    @Override
    public void broadcastNotification(String message) throws ExecutionException, InterruptedException {
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setType("BROADCAST");
        notification.setActive(true);
        notification.setCreatedAt(LocalDateTime.now().toString());
        
        notificationRepository.save(notification);
        
        // Log the administrative action
        logActivity("ADMIN", "SYSTEM", "BROADCAST_NOTIFICATION", "Sent message: " + message);
    }

    @Override
    public List<Notification> getAllNotifications() throws ExecutionException, InterruptedException {
        return notificationRepository.findAll();
    }

    @Override
    public void deleteNotification(String id) {
        notificationRepository.deleteById(id);
        logActivity("ADMIN", "SYSTEM", "DELETE_NOTIFICATION", "Deleted notification ID: " + id);
    }
}
