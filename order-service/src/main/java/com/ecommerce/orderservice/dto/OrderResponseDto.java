package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private UUID id;
    private UUID userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String paymentId;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private List<OrderItemResponseDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponseDto {
        private UUID productId;
        private int quantity;
        private BigDecimal price;
    }
}
