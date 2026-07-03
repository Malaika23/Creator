package com.ecommerce.notificationservice.messaging;

import com.ecommerce.common.event.OrderEvent;
import com.ecommerce.common.event.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationKafkaListener {

    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void consumeOrderEvent(OrderEvent event) {
        log.info("🔔 NOTIFICATION RECEIVED: Order Event [Type: {}, OrderId: {}, User: {}]", 
                event.getType(), event.getOrderId(), event.getUserId());
        
        switch (event.getType()) {
            case "ORDER_CREATED" -> sendEmail(event.getUserId().toString(), 
                    "Order Received!", 
                    "Thank you for your order. We are validating your items. Order ID: " + event.getOrderId());
            case "ORDER_CONFIRMED" -> sendEmail(event.getUserId().toString(), 
                    "Order Confirmed!", 
                    "Congratulations! Payment succeeded and order is confirmed. Order ID: " + event.getOrderId());
            case "ORDER_CANCELLED" -> sendEmail(event.getUserId().toString(), 
                    "Order Cancelled", 
                    "We regret to inform you that your order could not be completed. Order ID: " + event.getOrderId());
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void consumePaymentEvent(PaymentEvent event) {
        log.info("🔔 NOTIFICATION RECEIVED: Payment Event [Status: {}, OrderId: {}, Amount: {}]", 
                event.getStatus(), event.getOrderId(), event.getAmount());
    }

    private void sendEmail(String userId, String subject, String body) {
        // Dispatch simulation
        log.info("📩 Dispatching email simulation to user: {} | Subject: '{}' | Content: '{}'", 
                userId, subject, body);
    }
}
