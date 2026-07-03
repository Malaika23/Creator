package com.ecommerce.paymentservice.messaging;

import com.ecommerce.common.event.InventoryEvent;
import com.ecommerce.common.event.OrderEvent;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentKafkaListener {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @KafkaListener(topics = "order-events", groupId = "payment-group")
    public void consumeOrderCreatedEvent(OrderEvent event) {
        if ("ORDER_CREATED".equalsIgnoreCase(event.getType())) {
            log.info("Received ORDER_CREATED event for Order ID: {} with Total Amount: {}. Saving Payment Intent.", event.getOrderId(), event.getTotalAmount());
            
            // Save Payment Intent (pre-auth status)
            paymentRepository.findByOrderId(event.getOrderId()).orElseGet(() -> {
                Payment payment = Payment.builder()
                        .orderId(event.getOrderId())
                        .amount(event.getTotalAmount())
                        .status("PENDING")
                        .provider("MOCK")
                        .build();
                return paymentRepository.save(payment);
            });
        }
    }

    @KafkaListener(topics = "inventory-events", groupId = "payment-group")
    public void consumeInventoryReservedEvent(InventoryEvent event) {
        if ("INVENTORY_RESERVED".equalsIgnoreCase(event.getType()) || event.isSuccess()) {
            log.info("Received INVENTORY_RESERVED event for Order ID: {}. Processing Payment...", event.getOrderId());
            
            // Find saved Payment intent
            Payment payment = paymentRepository.findByOrderId(event.getOrderId())
                    .orElseThrow(() -> new IllegalStateException("Payment intent not found for Order ID: " + event.getOrderId()));

            if ("PENDING".equalsIgnoreCase(payment.getStatus())) {
                paymentService.processPayment(event.getOrderId(), payment.getAmount());
            } else {
                log.info("Payment for Order ID: {} has already been processed with status: {}", event.getOrderId(), payment.getStatus());
            }
        }
    }
}
