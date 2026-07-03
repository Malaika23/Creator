package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private UUID orderId;
    private UUID userId;
    private BigDecimal totalAmount;
    private List<OrderItem> items;
    private String type; // e.g., "ORDER_CREATED", "ORDER_CONFIRMED", "ORDER_CANCELLED"

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private UUID productId;
        private int quantity;
        private BigDecimal price;
    }
}
