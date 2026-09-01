package com.weverse.ticketing.domain.order.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weverse.ticketing.domain.order.dto.OrderEventPayload;
import com.weverse.ticketing.domain.order.entity.Order;
import com.weverse.ticketing.domain.order.repository.OrderRepository;
import com.weverse.ticketing.domain.product.entity.Product;
import com.weverse.ticketing.domain.product.repository.ProductRepository;
import com.weverse.ticketing.domain.user.entity.DeliveryAddress;
import com.weverse.ticketing.domain.user.entity.User;
import com.weverse.ticketing.domain.user.repository.DeliveryAddressRepository;
import com.weverse.ticketing.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * [백그라운드 주문 체결 Consumer]
 * - Kafka 토픽(order-requests)에서 주문 이벤트를 순차적으로 소비하여,
 *   MySQL DB에 안전하게 Order 엔티티를 INSERT하고 상품 재고를 영속화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderConsumer {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.order-requests:order-requests}", groupId = "order-fulfillment-group")
    @Transactional
    public void consumeOrderRequest(String message) {
        try {
            // 1. Kafka에서 받은 JSON 문자열을 OrderEventPayload DTO로 역직렬화
            OrderEventPayload payload = objectMapper.readValue(message, OrderEventPayload.class);
            log.info("[Kafka Consumer] 주문 이벤트 수신: OrderNumber={}, ProductId={}, UserId={}",
                    payload.getOrderNumber(), payload.getProductId(), payload.getUserId());

            // 2. MySQL DB에서 상품 조회 및 영속 계층 재고 차감
            Product product = productRepository.findById(payload.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + payload.getProductId()));

            product.decreaseStock(payload.getQuantity());

            // 3. User 및 DeliveryAddress 엔티티 조회 (프록시 또는 기본 엔티티)
            User user = userRepository.findById(payload.getUserId()).orElse(null);
            DeliveryAddress deliveryAddress = null;
            if (payload.getDeliveryAddressId() != null) {
                deliveryAddress = deliveryAddressRepository.findById(payload.getDeliveryAddressId()).orElse(null);
            }

            // 4. 결제 총액 계산 (상품 단가 * 수량)
            BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(payload.getQuantity()));

            // 5. Order 엔티티 생성 및 DB INSERT (PAID 상태로 체결 완료)
            Order order = Order.builder()
                    .orderNumber(payload.getOrderNumber())
                    .user(user)
                    .product(product)
                    .deliveryAddress(deliveryAddress)
                    .quantity(payload.getQuantity())
                    .totalAmount(totalAmount)
                    .status(Order.OrderStatus.PAID)
                    .build();

            orderRepository.save(order);

            log.info("[Kafka Consumer] DB 주문 체결 및 영속화 완료: OrderNumber={}, TotalAmount={}",
                    payload.getOrderNumber(), totalAmount);

        } catch (Exception e) {
            log.error("[Kafka Consumer Error] 주문 이벤트 소비 중 장애 발생: {}", e.getMessage(), e);
        }
    }
}
