package com.bookvault.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderStrategyFactory {

    private final Map<String, OrderProcessingStrategy> strategies = new HashMap<>();

    @Autowired
    public OrderStrategyFactory(List<OrderProcessingStrategy> strategyList) {
        for (OrderProcessingStrategy strategy : strategyList) {
            for (String type : strategy.getOrderTypes()) {
                strategies.put(type, strategy);
            }
        }
    }

    public OrderProcessingStrategy getStrategy(String orderType) {
        OrderProcessingStrategy strategy = strategies.get(orderType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported order type: " + orderType);
        }
        return strategy;
    }
}
