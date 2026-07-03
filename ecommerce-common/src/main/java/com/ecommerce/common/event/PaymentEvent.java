package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private UUID orderId;
    private UUID paymentId;
    private BigDecimal amount;
    private String status; // "SUCCESS", "FAILED"
    private String message;
    private String type; // e.g. "PAYMENT_PROCESSED", "PAYMENT_FAILED"
}
