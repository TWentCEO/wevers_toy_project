package com.weverse.ticketing.domain.order.event;

import com.weverse.ticketing.domain.order.dto.OrderEventPayload;

public interface OrderEventProducer {

    /**
     * 선착순 주문 비동기 처리: Kafka 이벤트 발행 뼈대 인터페이스
     * @param topic 발행할 Kafka 토픽명
     * @param payload 주문 이벤트 정보
     */
    void sendOrderRequestEvent(String topic, OrderEventPayload payload);
}
