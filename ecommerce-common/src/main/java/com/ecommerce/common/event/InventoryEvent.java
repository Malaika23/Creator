package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
    private UUID orderId;
    private boolean success;
    private String message;
    private List<InventoryItem> items;
    private String type; // e.g. "INVENTORY_RESERVED", "INVENTORY_RESERVATION_FAILED", "INVENTORY_RELEASED"

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        private UUID productId;
        private int quantity;
    }
}
