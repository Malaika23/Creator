package com.ecommerce.orderservice.messaging;

import com.ecommerce.common.event.InventoryEvent;
import com.ecommerce.common.event.PaymentEvent;
import com.ecommerce.orderservice.dto.OrderResponseDto;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderKafkaListenerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderKafkaListener orderKafkaListener;

    @Test
    public void testConsumePaymentEvent_Success() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .amount(BigDecimal.valueOf(100))
                .status("SUCCESS")
                .build();

        // Act
        orderKafkaListener.consumePaymentEvent(event);

        // Assert
        verify(orderService).confirmOrder(orderId, paymentId.toString());
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    public void testConsumePaymentEvent_Failed_EmitsCompensatingEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.builder()
                .orderId(orderId)
                .amount(BigDecimal.valueOf(100))
                .status("FAILED")
                .message("Card declined")
                .build();

        OrderResponseDto.OrderItemResponseDto itemDto = OrderResponseDto.OrderItemResponseDto.builder()
                .productId(productId)
                .quantity(3)
                .price(BigDecimal.valueOf(50))
                .build();

        OrderResponseDto orderResponse = OrderResponseDto.builder()
                .id(orderId)
                .items(Collections.singletonList(itemDto))
                .build();

        when(orderService.getOrderById(orderId)).thenReturn(orderResponse);

        // Act
        orderKafkaListener.consumePaymentEvent(event);

        // Assert
        verify(orderService).cancelOrder(orderId, "Card declined");
        verify(orderService).getOrderById(orderId);

        ArgumentCaptor<InventoryEvent> eventCaptor = ArgumentCaptor.forClass(InventoryEvent.class);
        verify(kafkaTemplate).send(eq("inventory-events"), eq(orderId.toString()), eventCaptor.capture());

        InventoryEvent rollbackEvent = eventCaptor.getValue();
        assertNotNull(rollbackEvent);
        assertEquals(orderId, rollbackEvent.getOrderId());
        assertEquals("INVENTORY_RELEASE", rollbackEvent.getType());
        assertEquals(1, rollbackEvent.getItems().size());
        assertEquals(productId, rollbackEvent.getItems().get(0).getProductId());
        assertEquals(3, rollbackEvent.getItems().get(0).getQuantity());
    }

    @Test
    public void testConsumeInventoryEvent_Failed_CancelsOrder() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        InventoryEvent event = InventoryEvent.builder()
                .orderId(orderId)
                .success(false)
                .type("INVENTORY_RESERVATION_FAILED")
                .message("No stock left")
                .build();

        // Act
        orderKafkaListener.consumeInventoryEvent(event);

        // Assert
        verify(orderService).cancelOrder(orderId, "No stock left");
    }
}
