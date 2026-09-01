package com.weverse.ticketing.domain.order.service;

import com.weverse.ticketing.domain.order.dto.CreateOrderRequestDto;
import com.weverse.ticketing.domain.order.dto.CreateOrderResponseDto;

public interface OrderCommandService {

    /**
     * 선착순 주문 비동기 요청 진입점
     * - 활성 토큰(Queue Token) 검증
     * - Redis 원자적 재고 선점 (DECR)
     * - Kafka 주문 이벤트 발행 -> 202 ACCEPTED 즉시 응답
     */
    CreateOrderResponseDto createOrderAsync(String queueToken, CreateOrderRequestDto requestDto);
}
