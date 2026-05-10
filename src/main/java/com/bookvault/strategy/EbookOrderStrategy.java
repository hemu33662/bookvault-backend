package com.bookvault.strategy;

import com.bookvault.entity.BookAccess;
import com.bookvault.entity.Order;
import com.bookvault.repository.BookAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class EbookOrderStrategy implements OrderProcessingStrategy {

    @Autowired
    private BookAccessRepository bookAccessRepository;

    @Override
    public void processOrder(Order order) throws ExecutionException, InterruptedException {
        // Ebooks get immediate digital access
        BookAccess access = BookAccess.builder()
                .userId(order.getUserId())
                .bookId(order.getBookId())
                .accessType("FULL_DIGITAL")
                .status("ACTIVE")
                .grantedAt(LocalDateTime.now().toString())
                .build();
        bookAccessRepository.save(access);
    }

    @Override
    public List<String> getOrderTypes() {
        return List.of("EBOOK_REQUEST", "PAID_EBOOK", "FREE");
    }
}
