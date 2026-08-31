package com.weverse.ticketing.domain.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weverse.ticketing.domain.order.dto.OrderEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderEventProducer implements OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void sendOrderRequestEvent(String topic, OrderEventPayload payload) {
        // TODO: [학습자 주도 영역] 논리 적용 필요 - 비동기 큐 동시성 제어: Kafka 파티션 키 전략 및 전송 실패 보상 로직 설계
        try {
            String jsonMessage = objectMapper.writeValueAsString(payload);
            // 메시지 순서 및 파티션 분배를 위해 productId 또는 orderNumber를 Key로 전송
            kafkaTemplate.send(topic, String.valueOf(payload.getProductId()), jsonMessage);
            log.info("주문 요청 Kafka 이벤트 발행 완료 - Topic: {}, OrderNumber: {}", topic, payload.getOrderNumber());
        } catch (Exception e) {
            log.error("Kafka 주문 이벤트 발행 실패 - OrderNumber: {}", payload.getOrderNumber(), e);
            throw new RuntimeException("주문 이벤트 발행 중 오류가 발생했습니다.", e);
        }
    }
}
