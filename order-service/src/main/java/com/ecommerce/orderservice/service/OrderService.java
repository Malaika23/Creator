package com.ecommerce.orderservice.service;

import com.ecommerce.common.event.OrderEvent;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.dto.OrderRequestDto;
import com.ecommerce.orderservice.dto.OrderResponseDto;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String ORDER_TOPIC = "order-events";

    @Transactional
    public OrderResponseDto createOrder(UUID userId, OrderRequestDto request) {
        // 1. Idempotency Check
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingOrder.isPresent()) {
            log.info("Duplicate request intercepted for idempotency key: {}. Returning existing order details.", request.getIdempotencyKey());
            return mapToResponseDto(existingOrder.get());
        }

        // 2. Calculate Total Amount
        BigDecimal total = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Construct Order Entity
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(total)
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build();

        List<OrderItem> items = request.getItems().stream()
                .map(itemDto -> OrderItem.builder()
                        .order(order)
                        .productId(itemDto.getProductId())
                        .quantity(itemDto.getQuantity())
                        .price(itemDto.getPrice())
                        .build())
                .collect(Collectors.toList());

        order.setItems(items);
        Order savedOrder = orderRepository.save(order);

        // 4. Emit OrderCreatedEvent to Kafka
        OrderEvent event = OrderEvent.builder()
                .orderId(savedOrder.getId())
                .userId(userId)
                .totalAmount(savedOrder.getTotalAmount())
                .type("ORDER_CREATED")
                .items(savedOrder.getItems().stream()
                        .map(item -> OrderEvent.OrderItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        log.info("Emitting ORDER_CREATED event to Kafka for order ID: {}", savedOrder.getId());
        kafkaTemplate.send(ORDER_TOPIC, savedOrder.getId().toString(), event);

        return mapToResponseDto(savedOrder);
    }

    public OrderResponseDto getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToResponseDto(order);
    }

    public List<OrderResponseDto> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void confirmOrder(UUID orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentId(paymentId);
        orderRepository.save(order);
        log.info("Order ID: {} status updated to CONFIRMED with payment ID: {}", orderId, paymentId);
    }

    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order ID: {} status updated to CANCELLED. Reason: {}", orderId, reason);
    }

    private OrderResponseDto mapToResponseDto(Order order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .paymentId(order.getPaymentId())
                .idempotencyKey(order.getIdempotencyKey())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream()
                        .map(item -> OrderResponseDto.OrderItemResponseDto.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
