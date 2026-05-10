package com.bookvault.strategy;

import com.bookvault.entity.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class PaperbackOrderStrategy implements OrderProcessingStrategy {

    @Override
    public void processOrder(Order order) throws ExecutionException, InterruptedException {
        // Paperback orders do not get immediate digital access.
        // Instead, we might initiate shipping logic here, connect to a logistics API, etc.
        System.out.println("Initiating shipping process for order: " + order.getId() + " to address: " + order.getShippingAddress());
    }

    @Override
    public List<String> getOrderTypes() {
        return List.of("PAPERBACK_ORDER", "PAPERBACK", "PHYSICAL");
    }
}
