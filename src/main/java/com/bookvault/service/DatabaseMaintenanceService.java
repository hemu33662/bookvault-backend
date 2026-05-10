package com.bookvault.service;

import com.bookvault.entity.*;
import com.bookvault.repository.*;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DatabaseMaintenanceService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private OrderRepository orderRepository;

    public void initializeDatabase() throws Exception {
        System.out.println("Initializing database collections...");

        // 1. Seed Admin User
        if (userRepository.findByFirebaseUid("admin-master-uid").isEmpty()) {
            User admin = User.builder()
                    .firebaseUid("admin-master-uid")
                    .email("admin@bookvault.com")
                    .role(Role.ADMIN)
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now().toString())
                    .build();
            userRepository.save(admin);
            System.out.println("Admin user seeded.");
        }

        // 2. Seed Sample Books (if none exist)
        if (bookRepository.findAll().isEmpty()) {
            Book sampleBook = Book.builder()
                    .title("The Digital Library")
                    .author("Antigravity")
                    .description("A guide to modern book management.")
                    .price(new BigDecimal("24.99"))
                    .type(BookType.PAID_EBOOK)
                    .status("PUBLISHED")
                    .coverImageUrl("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c")
                    .createdAt(LocalDateTime.now().toString())
                    .build();
            bookRepository.save(sampleBook);
            System.out.println("Sample book seeded.");
        }

        // 3. Seed Initial Activity
        Activity initActivity = Activity.builder()
                .action("SYSTEM_INIT")
                .detail("Database collections initialized successfully.")
                .timestamp(LocalDateTime.now().toString())
                .userName("System")
                .build();
        activityRepository.save(initActivity);
        System.out.println("System activity logged.");

        // 4. Seed a Placeholder Order (optional, but helps recreate the collection)
        if (orderRepository.findAll().isEmpty()) {
            Order placeholderOrder = Order.builder()
                    .bookId("sample-book-id")
                    .userId("admin-master-uid")
                    .type("EBOOK_REQUEST")
                    .status("INITIALIZED")
                    .createdAt(LocalDateTime.now().toString())
                    .referenceId("INIT-0001")
                    .build();
            orderRepository.save(placeholderOrder);
            System.out.println("Placeholder order seeded.");
        }

        System.out.println("Database initialization complete. Collections have been recreated.");
    }
}
