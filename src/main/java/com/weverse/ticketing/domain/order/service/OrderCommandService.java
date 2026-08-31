package com.weverse.ticketing.domain.order.service;

import com.weverse.ticketing.domain.order.dto.CreateOrderRequestDto;
import com.weverse.ticketing.domain.order.dto.CreateOrderResponseDto;

public interface OrderCommandService {

    /**
     * 선착순 주문 비동기 요청 진입점
     * DB에 동기 INSERT를 하지 않고 이벤트 발행을 통해 처리함
     */
    CreateOrderResponseDto createOrderAsync(CreateOrderRequestDto requestDto);
}
