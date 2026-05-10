package com.bookvault.strategy;

import com.bookvault.entity.Order;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface OrderProcessingStrategy {
    void processOrder(Order order) throws ExecutionException, InterruptedException;
    List<String> getOrderTypes();
}
