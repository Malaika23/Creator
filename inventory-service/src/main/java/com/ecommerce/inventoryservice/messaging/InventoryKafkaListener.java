package com.ecommerce.inventoryservice.messaging;

import com.ecommerce.common.event.InventoryEvent;
import com.ecommerce.common.event.OrderEvent;
import com.ecommerce.inventoryservice.dto.ReservationResponseDto;
import com.ecommerce.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryKafkaListener {

    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String INVENTORY_TOPIC = "inventory-events";

    @KafkaListener(topics = "order-events", groupId = "inventory-group")
    public void consumeOrderEvent(OrderEvent event) {
        if ("ORDER_CREATED".equalsIgnoreCase(event.getType())) {
            log.info("Received ORDER_CREATED event for Order ID: {}. Processing stock reservation...", event.getOrderId());
            
            boolean allReserved = true;
            String errorMessage = "";
            
            // Track reservations to roll back if some items fail
            List<OrderEvent.OrderItem> reservedItems = new ArrayList<>();

            for (OrderEvent.OrderItem item : event.getItems()) {
                // Reserve stock using the high-concurrency Redis Lock method
                ReservationResponseDto res = inventoryService.reserveWithRedisLock(item.getProductId(), item.getQuantity());
                if (res.isSuccess()) {
                    reservedItems.add(item);
                } else {
                    allReserved = false;
                    errorMessage = res.getMessage();
                    break;
                }
            }

            if (allReserved) {
                // Success: Emit INVENTORY_RESERVED event
                InventoryEvent successEvent = InventoryEvent.builder()
                        .orderId(event.getOrderId())
                        .success(true)
                        .type("INVENTORY_RESERVED")
                        .message("Successfully reserved stock")
                        .items(event.getItems().stream()
                                .map(item -> InventoryEvent.InventoryItem.builder()
                                        .productId(item.getProductId())
                                        .quantity(item.getQuantity())
                                        .build())
                                .collect(Collectors.toList()))
                        .build();

                log.info("Stock reservation successful. Emitting INVENTORY_RESERVED for Order ID: {}", event.getOrderId());
                kafkaTemplate.send(INVENTORY_TOPIC, event.getOrderId().toString(), successEvent);
            } else {
                // Fail: Rollback already reserved items in this order
                for (OrderEvent.OrderItem item : reservedItems) {
                    inventoryService.releaseStock(item.getProductId(), item.getQuantity());
                }

                // Emit INVENTORY_RESERVATION_FAILED event
                InventoryEvent failEvent = InventoryEvent.builder()
                        .orderId(event.getOrderId())
                        .success(false)
                        .type("INVENTORY_RESERVATION_FAILED")
                        .message("Failed to reserve stock: " + errorMessage)
                        .build();

                log.warn("Stock reservation failed for Order ID: {}. Emitting INVENTORY_RESERVATION_FAILED", event.getOrderId());
                kafkaTemplate.send(INVENTORY_TOPIC, event.getOrderId().toString(), failEvent);
            }
        }
    }

    @KafkaListener(topics = "inventory-events", groupId = "inventory-group")
    public void consumeCompensatingEvent(InventoryEvent event) {
        if ("INVENTORY_RELEASE".equalsIgnoreCase(event.getType())) {
            log.info("Received INVENTORY_RELEASE compensating transaction request for Order ID: {}", event.getOrderId());
            if (event.getItems() != null) {
                for (InventoryEvent.InventoryItem item : event.getItems()) {
                    inventoryService.releaseStock(item.getProductId(), item.getQuantity());
                }
            }
        }
    }
}
