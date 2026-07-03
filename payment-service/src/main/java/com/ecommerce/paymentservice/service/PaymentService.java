package com.ecommerce.paymentservice.service;

import com.ecommerce.common.event.PaymentEvent;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String PAYMENT_TOPIC = "payment-events";

    @Transactional
    public Payment processPayment(UUID orderId, BigDecimal amount) {
        log.info("Processing payment for Order ID: {} and Amount: {}", orderId, amount);

        // 1. Check duplicate payments
        return paymentRepository.findByOrderId(orderId)
                .map(existingPayment -> {
                    log.info("Found existing payment for Order ID: {}. Returning status: {}", orderId, existingPayment.getStatus());
                    return existingPayment;
                })
                .orElseGet(() -> {
                    // Create pending payment record
                    Payment payment = Payment.builder()
                            .orderId(orderId)
                            .amount(amount)
                            .status("PENDING")
                            .provider("MOCK")
                            .build();
                    Payment saved = paymentRepository.save(payment);

                    // 2. Process Mock Gateway Transaction
                    // Mock Logic: If order amount is exactly 999.00 (or above 10,000), simulate payment failure!
                    boolean success = amount.compareTo(BigDecimal.valueOf(10000)) < 0 && amount.compareTo(BigDecimal.valueOf(999)) != 0;

                    if (success) {
                        saved.setStatus("SUCCESS");
                        saved.setTransactionReference("txn_" + UUID.randomUUID().toString().substring(0, 8));
                    } else {
                        saved.setStatus("FAILED");
                    }
                    Payment finalPayment = paymentRepository.save(saved);

                    // 3. Emit PaymentEvent to Kafka
                    PaymentEvent event = PaymentEvent.builder()
                            .orderId(orderId)
                            .paymentId(finalPayment.getId())
                            .amount(amount)
                            .status(finalPayment.getStatus())
                            .type("PAYMENT_PROCESSED")
                            .message(success ? "Payment successfully captured" : "Payment authorization failed")
                            .build();

                    log.info("Emitting PAYMENT_PROCESSED event for Order ID: {} with Status: {}", orderId, finalPayment.getStatus());
                    kafkaTemplate.send(PAYMENT_TOPIC, orderId.toString(), event);

                    return finalPayment;
                });
    }
}
