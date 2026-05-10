package com.bookvault.service;

import com.bookvault.entity.Book;
import com.bookvault.entity.Order;
import com.bookvault.entity.User;
import com.bookvault.repository.BookRepository;
import com.bookvault.repository.OrderRepository;
import com.bookvault.repository.UserRepository;
import com.bookvault.strategy.OrderProcessingStrategy;
import com.bookvault.strategy.OrderStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private OrderStrategyFactory orderStrategyFactory;

    @Autowired
    private MailServiceClient mailServiceClient;

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Value("${mail-service.admin-email}")
    private String adminEmail;

    public Order createOrder(Order order) throws ExecutionException, InterruptedException {
        order.setReferenceId("BV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now().toString());
        order.setUpdatedAt(LocalDateTime.now().toString());

        Order savedOrder = orderRepository.save(order);

        // Log activity instead of sending mail
        adminDashboardService.logActivity(savedOrder.getUserId(), savedOrder.getCustomerName(), "ORDER_CREATED", 
                "New order " + savedOrder.getReferenceId() + " placed for book: " + savedOrder.getBookId());
        
        // Internal record keeping replaces external mail
        sendOrderEmailToUser(savedOrder);
        sendOrderEmailToAdmin(savedOrder);

        return savedOrder;
    }

    public Order updateOrderStatus(String orderId, String status) throws ExecutionException, InterruptedException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now().toString());
        Order updatedOrder = orderRepository.save(order);

        // If completed, process using the strategy based on the order type
        if ("COMPLETED".equalsIgnoreCase(status)) {
            OrderProcessingStrategy strategy = orderStrategyFactory.getStrategy(updatedOrder.getType());
            strategy.processOrder(updatedOrder);
        }

        // Log activity instead of sending mail
        adminDashboardService.logActivity(updatedOrder.getUserId(), "SYSTEM", "ORDER_STATUS_UPDATE", 
                "Order " + updatedOrder.getReferenceId() + " status updated to: " + status);
        
        sendOrderEmail(updatedOrder, "Order Status Updated: " + status);

        return updatedOrder;
    }

    public List<Order> getOrdersByUserId(String userId) throws ExecutionException, InterruptedException {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getAllOrders() throws ExecutionException, InterruptedException {
        return orderRepository.findAll();
    }

    private void sendOrderEmailToUser(Order order) {
        try {
            Optional<User> userOpt = userRepository.findById(order.getUserId());
            Optional<Book> bookOpt = bookRepository.findById(order.getBookId());

            if (userOpt.isPresent() && bookOpt.isPresent()) {
                User user = userOpt.get();
                Book book = bookOpt.get();
                String name = order.getCustomerName() != null ? order.getCustomerName() : user.getEmail();
                
                String body = String.format(
                        "<div style='font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px;'>" +
                        "<h2 style='color: #1a1a2e;'>Hello %s,</h2>" +
                        "<p>Thank you for your order! Our team will reach out to you soon.</p>" +
                        "<div style='background: #f8fafc; padding: 15px; border-radius: 8px; margin: 20px 0;'>" +
                        "<h3>Order Reference ID: <span style='color: #6366f1;'>%s</span></h3>" +
                        "<h4>Book Details:</h4>" +
                        "<ul>" +
                        "<li><strong>Title:</strong> %s</li>" +
                        "<li><strong>Author:</strong> %s</li>" +
                        "<li><strong>Price:</strong> %s</li>" +
                        "<li><strong>Book Type:</strong> %s</li>" +
                        "<li><strong>Ordered On:</strong> %s</li>" +
                        "</ul>" +
                        "</div>" +
                        "<p>Your details are secure with us.</p>" +
                        "<p>Best Regards,<br>The BookVault Team</p>" +
                        "</div>",
                        name, order.getReferenceId(), book.getTitle(), book.getAuthor(), 
                        (book.getPrice() == null || book.getPrice().compareTo(BigDecimal.ZERO) == 0) ? "Free" : "$" + book.getPrice(),
                        order.getType(), order.getCreatedAt()
                );
                
                if (order.getMessage() != null && !order.getMessage().trim().isEmpty()) {
                    body += String.format("<p><strong>Your Message:</strong> %s</p>", order.getMessage());
                }
                
                body += "<p>Your details are secure with us.</p>" +
                        "<p>Best Regards,<br>The BookVault Team</p>" +
                        "</div>";
                        
                /*
                mailServiceClient.sendEmail(user.getEmail(), "BookVault: Order Received - " + order.getReferenceId(), body, user.getFirebaseUid());
                */
                log.info("Internal Notification (User): BookVault Order Received - {}", order.getReferenceId());
            }
        } catch (Exception e) {
            System.err.println("Failed to send order email to user: " + e.getMessage());
        }
    }

    private void sendOrderEmailToAdmin(Order order) {
        try {
            Optional<Book> bookOpt = bookRepository.findById(order.getBookId());
            Optional<User> userOpt = userRepository.findById(order.getUserId());

            if (bookOpt.isPresent() && userOpt.isPresent()) {
                Book book = bookOpt.get();
                User user = userOpt.get();
                
                String body = String.format(
                        "<div style='font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px;'>" +
                        "<h2 style='color: #1a1a2e;'>New Order Received</h2>" +
                        "<p>A new order has been placed on BookVault.</p>" +
                        "<div style='background: #f8fafc; padding: 15px; border-radius: 8px; margin: 20px 0;'>" +
                        "<h3>Reference ID: %s</h3>" +
                        "<h4>Customer Details:</h4>" +
                        "<ul>" +
                        "<li><strong>Form Name:</strong> %s</li>" +
                        "<li><strong>WhatsApp:</strong> %s</li>" +
                        "<li><strong>User Email:</strong> %s</li>" +
                        "<li><strong>User ID:</strong> %s</li>" +
                        "</ul>" +
                        "<h4>Book Details:</h4>" +
                        "<ul>" +
                        "<li><strong>Title:</strong> %s</li>" +
                        "<li><strong>Author:</strong> %s</li>" +
                        "<li><strong>Price:</strong> %s</li>" +
                        "<li><strong>Type:</strong> %s</li>" +
                        "</ul>" +
                        "</div>" +
                        "</div>",
                        order.getReferenceId(), order.getCustomerName(), order.getWhatsappNumber(), user.getEmail(), order.getUserId(),
                        book.getTitle(), book.getAuthor(), 
                        (book.getPrice() == null || book.getPrice().compareTo(BigDecimal.ZERO) == 0) ? "Free" : "$" + book.getPrice(),
                        order.getType()
                );
                
                if (order.getMessage() != null && !order.getMessage().trim().isEmpty()) {
                    body += String.format("<p><strong>Customer Message:</strong> %s</p>", order.getMessage());
                }
                
                body += "</div></div>";
                
                /*
                mailServiceClient.sendEmail(adminEmail, "ADMIN: New Order - " + order.getReferenceId(), body, "SYSTEM");
                */
                adminDashboardService.logActivity(order.getUserId(), order.getCustomerName(), "ORDER_NOTIFICATION_DETAILS", body);
                log.info("Internal Notification (Admin): New Order - {}", order.getReferenceId());
            }
        } catch (Exception e) {
            System.err.println("Failed to send admin notification: " + e.getMessage());
        }
    }

    private void sendOrderEmail(Order order, String subject) {
        // Keeping legacy method for other status updates if needed
        try {
            Optional<User> userOpt = userRepository.findById(order.getUserId());
            Optional<Book> bookOpt = bookRepository.findById(order.getBookId());

            if (userOpt.isPresent() && bookOpt.isPresent()) {
                User user = userOpt.get();
                Book book = bookOpt.get();
                String body = String.format(
                        "<h1>%s</h1>" +
                        "<p>Hello %s,</p>" +
                        "<p>Your order for <strong>%s</strong> is now <strong>%s</strong>.</p>" +
                        "<p>Reference ID: %s</p>" +
                        "<p>Thank you for using BookVault!</p>",
                        subject, user.getEmail(), book.getTitle(), order.getStatus(), order.getReferenceId()
                );
                /*
                mailServiceClient.sendEmail(user.getEmail(), "BookVault: " + subject, body, user.getFirebaseUid());
                */
                log.info("Internal Notification (Status): {} for Order {}", subject, order.getReferenceId());
            }
        } catch (Exception e) {
            System.err.println("Failed to send order email: " + e.getMessage());
        }
    }
}
