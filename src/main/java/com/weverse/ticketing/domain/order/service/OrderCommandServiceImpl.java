package com.weverse.ticketing.domain.order.service;

import com.weverse.ticketing.domain.order.dto.CreateOrderRequestDto;
import com.weverse.ticketing.domain.order.dto.CreateOrderResponseDto;
import com.weverse.ticketing.domain.order.dto.OrderEventPayload;
import com.weverse.ticketing.domain.order.event.OrderEventProducer;
import com.weverse.ticketing.domain.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * [선착순 비동기 주문 Command 서비스]
 * - DB에 직접 동기식 INSERT를 하지 않고,
 *   1) Redis 원자적 재고 선점(DECR) -> 2) Kafka 이벤트 발행 -> 3) 202 ACCEPTED 즉시 응답
 *   의 3단계 초고속 파이프라인으로 동작합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandServiceImpl implements OrderCommandService {

    private final OrderEventProducer orderEventProducer;
    private final QueueService queueService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.kafka.topics.order-requests:order-requests}")
    private String orderRequestsTopic;

    private static final String PRODUCT_STOCK_KEY_PREFIX = "product:stock:";
    private static final String ACTIVE_TOKEN_KEY_PREFIX = "queue:token:";

    @Override
    public CreateOrderResponseDto createOrderAsync(String queueToken, CreateOrderRequestDto requestDto) {
        // [Step 1: 활성 대기열 토큰 검증]
        // 대기열을 정상적으로 거쳐서 승급된 유효한 ACTIVE 토큰인지 확인합니다.
        boolean isValidToken = queueService.validateActiveToken(queueToken, requestDto.getProductId());
        if (!isValidToken) {
            log.warn("[Order Denied] 유효하지 않거나 만료된 대기열 토큰입니다. Token: {}", queueToken);
            throw new IllegalArgumentException("대기열 토큰이 유효하지 않거나 만료되었습니다. 다시 대기열에 진입해주세요.");
        }

        // [Step 2: Redis In-Memory 원자적 재고 선점 (Atomic Decrement)]
        // 싱글 스레드로 동작하는 Redis DECRBY 명령어를 사용하여 0.001초 만에 재고를 깎습니다.
        String stockKey = PRODUCT_STOCK_KEY_PREFIX + requestDto.getProductId();
        long quantity = requestDto.getQuantity();

        Long remainingStock = redisTemplate.opsForValue().decrement(stockKey, quantity);

        // 만약 깎고 난 뒤의 재고가 0 미만이라면 -> "완판/품절" 상태!
        if (remainingStock == null || remainingStock < 0) {
            // 내가 깎아서 음수가 된 수량을 즉시 원상복구(Rollback)시킵니다.
            redisTemplate.opsForValue().increment(stockKey, quantity);
            log.warn("[Order Sold Out] 상품 품절로 인한 주문 실패: ProductId={}", requestDto.getProductId());
            throw new IllegalStateException("상품의 잔여 재고가 부족하여 주문할 수 없습니다 (품절).");
        }

        // [Step 3: 주문 번호 생성 및 비동기 이벤트 발행]
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();

        OrderEventPayload payload = OrderEventPayload.builder()
                .orderNumber(orderNumber)
                .userId(requestDto.getUserId())
                .productId(requestDto.getProductId())
                .deliveryAddressId(requestDto.getDeliveryAddressId())
                .quantity(requestDto.getQuantity())
                .requestedAt(LocalDateTime.now())
                .build();

        // Kafka 토픽(order-requests)으로 비동기 발행 (Partition Key: productId로 상품별 순서 보장)
        orderEventProducer.sendOrderRequestEvent(orderRequestsTopic, payload);

        // [Step 4: 1회용 활성 토큰 소각 처리]
        // 한 번 주문에 사용된 토큰은 즉시 삭제하여 재사용(중복 구매 어뷰징)을 방지합니다.
        if (queueToken != null && !queueToken.isBlank()) {
            redisTemplate.delete(ACTIVE_TOKEN_KEY_PREFIX + queueToken);
        }

        log.info("[Order Accepted] 주문 접수 완료 (202 ACCEPTED) - OrderNumber: {}, 잔여재고: {}", orderNumber, remainingStock);

        return CreateOrderResponseDto.accepted(orderNumber);
    }
}
