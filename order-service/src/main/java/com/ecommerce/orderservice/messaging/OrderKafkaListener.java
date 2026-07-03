package com.ecommerce.orderservice.messaging;

import com.ecommerce.common.event.InventoryEvent;
import com.ecommerce.common.event.PaymentEvent;
import com.ecommerce.orderservice.dto.OrderResponseDto;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaListener {

    private final OrderService orderService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String INVENTORY_TOPIC = "inventory-events";

    @KafkaListener(topics = "payment-events", groupId = "order-group")
    public void consumePaymentEvent(PaymentEvent event) {
        log.info("Received PaymentEvent for Order ID: {} with Status: {}", event.getOrderId(), event.getStatus());
        if ("SUCCESS".equalsIgnoreCase(event.getStatus())) {
            orderService.confirmOrder(event.getOrderId(), event.getPaymentId().toString());
        } else {
            orderService.cancelOrder(event.getOrderId(), event.getMessage());
            
            // Compensating Transaction: Roll back/release inventory!
            try {
                OrderResponseDto order = orderService.getOrderById(event.getOrderId());
                List<InventoryEvent.InventoryItem> rollbackItems = order.getItems().stream()
                        .map(item -> InventoryEvent.InventoryItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList());

                InventoryEvent rollbackEvent = InventoryEvent.builder()
                        .orderId(event.getOrderId())
                        .type("INVENTORY_RELEASE")
                        .message("Payment failed; roll back reserved stock")
                        .items(rollbackItems)
                        .build();

                log.info("Emitting INVENTORY_RELEASE event for Order ID: {} with {} items", event.getOrderId(), rollbackItems.size());
                kafkaTemplate.send(INVENTORY_TOPIC, event.getOrderId().toString(), rollbackEvent);
            } catch (Exception e) {
                log.error("Failed to fetch order details for emitting compensating transaction for Order ID: {}", event.getOrderId(), e);
            }
        }
    }

    @KafkaListener(topics = "inventory-events", groupId = "order-group")
    public void consumeInventoryEvent(InventoryEvent event) {
        // If inventory reservation failed, we cancel the order immediately.
        if ("INVENTORY_RESERVATION_FAILED".equalsIgnoreCase(event.getType()) || !event.isSuccess()) {
            log.warn("Inventory reservation failed for Order ID: {}. Cancelling order.", event.getOrderId());
            orderService.cancelOrder(event.getOrderId(), event.getMessage());
        }
    }
}
