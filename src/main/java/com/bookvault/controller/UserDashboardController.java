package com.bookvault.controller;

import com.bookvault.entity.Activity;
import com.bookvault.entity.Book;
import com.bookvault.entity.Order;
import com.bookvault.repository.ActivityRepository;
import com.bookvault.repository.BookRepository;
import com.bookvault.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class UserDashboardController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private com.bookvault.service.SecurityService securityService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() throws ExecutionException, InterruptedException {
        String userId = securityService.getCurrentUserFirebaseUid();
        if (userId == null) return ResponseEntity.status(401).build();

        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> userId.equals(o.getUserId()) && ("APPROVED".equals(o.getStatus()) || "COMPLETED".equals(o.getStatus())))
                .collect(Collectors.toList());

        List<Activity> activities = activityRepository.findByUser(userId, 100);
        
        long readCount = activities.stream()
                .filter(a -> "READ_BOOK".equals(a.getAction()))
                .map(Activity::getDetail)
                .distinct()
                .count();

        // Calculate active favorites by looking at the most recent action for each book
        Map<String, String> latestBookActions = activities.stream()
                .filter(a -> "FAVORITE_BOOK".equals(a.getAction()) || "UNFAVORITE_BOOK".equals(a.getAction()))
                .collect(Collectors.toMap(
                        Activity::getDetail,
                        Activity::getAction,
                        (existing, replacement) -> existing // Keep the most recent 
                ));

        long favCount = latestBookActions.values().stream()
                .filter(action -> "FAVORITE_BOOK".equals(action))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("purchased", orders.size());
        stats.put("read", readCount);
        stats.put("favorites", favCount);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user-favorites")
    public ResponseEntity<List<String>> getUserFavorites() throws ExecutionException, InterruptedException {
        String userId = securityService.getCurrentUserFirebaseUid();
        if (userId == null) return ResponseEntity.status(401).build();

        // Fetch last 500 activities to reconstruct favorite state
        List<Activity> activities = activityRepository.findByUser(userId, 500);
        
        // Use a Map to keep track of the latest action for each book
        Map<String, String> latestBookActions = activities.stream()
                .filter(a -> "FAVORITE_BOOK".equals(a.getAction()) || "UNFAVORITE_BOOK".equals(a.getAction()))
                .collect(Collectors.toMap(
                        Activity::getDetail,
                        Activity::getAction,
                        (existing, replacement) -> existing 
                ));

        List<String> favoriteBookIds = latestBookActions.entrySet().stream()
                .filter(e -> "FAVORITE_BOOK".equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(favoriteBookIds);
    }

    @GetMapping("/recent-reading")
    public ResponseEntity<List<Book>> getRecentlyRead() throws ExecutionException, InterruptedException {
        String userId = securityService.getCurrentUserFirebaseUid();
        if (userId == null) return ResponseEntity.status(401).build();

        List<Activity> activities = activityRepository.findByUser(userId, 50);
        
        // Extract unique book IDs from "READ_BOOK" actions
        List<String> recentBookIds = activities.stream()
                .filter(a -> "READ_BOOK".equals(a.getAction()))
                .map(Activity::getDetail) 
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        List<Book> recentBooks = new ArrayList<>();
        for (String bookId : recentBookIds) {
            Book book = bookRepository.findById(bookId).orElse(null);
            if (book != null) {
                recentBooks.add(book);
            }
        }
        
        return ResponseEntity.ok(recentBooks);
    }

    @PostMapping("/log-activity")
    public ResponseEntity<String> logUserActivity(@RequestBody Activity activity) {
        String userId = securityService.getCurrentUserFirebaseUid();
        if (userId == null) return ResponseEntity.status(401).build();
        
        activity.setUserId(userId);
        activity.setTimestamp(LocalDateTime.now().toString());
        activityRepository.save(activity);
        return ResponseEntity.ok("Activity logged");
    }
}
