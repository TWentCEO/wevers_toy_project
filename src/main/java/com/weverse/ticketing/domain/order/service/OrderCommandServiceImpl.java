package com.weverse.ticketing.domain.order.service;

import com.weverse.ticketing.domain.order.dto.CreateOrderRequestDto;
import com.weverse.ticketing.domain.order.dto.CreateOrderResponseDto;
import com.weverse.ticketing.domain.order.dto.OrderEventPayload;
import com.weverse.ticketing.domain.order.event.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandServiceImpl implements OrderCommandService {

    private final OrderEventProducer orderEventProducer;

    @Value("${app.kafka.topics.order-requests:order-requests}")
    private String orderRequestsTopic;

    @Override
    public CreateOrderResponseDto createOrderAsync(CreateOrderRequestDto requestDto) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();

        // TODO: [학습자 주도 영역] 논리 적용 필요 - 분산 락 / 비동기 큐 동시성 제어: Redis 대기열 검증 및 선착순 수량 선점 로직 적용

        OrderEventPayload payload = OrderEventPayload.builder()
                .orderNumber(orderNumber)
                .userId(requestDto.getUserId())
                .productId(requestDto.getProductId())
                .deliveryAddressId(requestDto.getDeliveryAddressId())
                .quantity(requestDto.getQuantity())
                .requestedAt(LocalDateTime.now())
                .build();

        orderEventProducer.sendOrderRequestEvent(orderRequestsTopic, payload);

        return CreateOrderResponseDto.accepted(orderNumber);
    }
}
