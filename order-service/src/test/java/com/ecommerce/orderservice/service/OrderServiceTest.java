package com.ecommerce.orderservice.service;

import com.ecommerce.common.event.OrderEvent;
import com.ecommerce.orderservice.dto.OrderItemRequestDto;
import com.ecommerce.orderservice.dto.OrderRequestDto;
import com.ecommerce.orderservice.dto.OrderResponseDto;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    @Test
    public void testCreateOrder_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-123";

        OrderItemRequestDto itemRequest = new OrderItemRequestDto();
        itemRequest.setProductId(productId);
        itemRequest.setQuantity(2);
        itemRequest.setPrice(BigDecimal.valueOf(100));

        OrderRequestDto request = new OrderRequestDto();
        request.setIdempotencyKey(idempotencyKey);
        request.setItems(Collections.singletonList(itemRequest));

        Order savedOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(BigDecimal.valueOf(200))
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();
        
        OrderItem savedItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .order(savedOrder)
                .productId(productId)
                .quantity(2)
                .price(BigDecimal.valueOf(100))
                .build();
        savedOrder.setItems(Collections.singletonList(savedItem));

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        OrderResponseDto response = orderService.createOrder(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(savedOrder.getId(), response.getId());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals(BigDecimal.valueOf(200), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(productId, response.getItems().get(0).getProductId());

        verify(orderRepository).findByIdempotencyKey(idempotencyKey);
        verify(orderRepository).save(any(Order.class));
        
        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(kafkaTemplate).send(eq("order-events"), eq(savedOrder.getId().toString()), eventCaptor.capture());
        
        OrderEvent sentEvent = eventCaptor.getValue();
        assertEquals(savedOrder.getId(), sentEvent.getOrderId());
        assertEquals(userId, sentEvent.getUserId());
        assertEquals("ORDER_CREATED", sentEvent.getType());
        assertEquals(1, sentEvent.getItems().size());
        assertEquals(productId, sentEvent.getItems().get(0).getProductId());
        assertEquals(2, sentEvent.getItems().get(0).getQuantity());
    }

    @Test
    public void testCreateOrder_DuplicateIdempotencyKey() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "key-duplicate";

        OrderRequestDto request = new OrderRequestDto();
        request.setIdempotencyKey(idempotencyKey);
        request.setItems(Collections.emptyList());

        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(BigDecimal.valueOf(500))
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .items(Collections.emptyList())
                .build();

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingOrder));

        // Act
        OrderResponseDto response = orderService.createOrder(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(existingOrder.getId(), response.getId());
        assertEquals(BigDecimal.valueOf(500), response.getTotalAmount());

        verify(orderRepository).findByIdempotencyKey(idempotencyKey);
        verifyNoMoreInteractions(orderRepository);
        verifyNoInteractions(kafkaTemplate);
    }
}
