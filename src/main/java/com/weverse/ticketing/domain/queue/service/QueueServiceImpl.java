package com.weverse.ticketing.domain.queue.service;

import com.weverse.ticketing.domain.queue.dto.QueueEnterRequestDto;
import com.weverse.ticketing.domain.queue.dto.QueueResponseDto;
import com.weverse.ticketing.domain.queue.dto.QueueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final StringRedisTemplate redisTemplate;

    private static final String WAITING_QUEUE_KEY_PREFIX = "queue:product:";
    private static final String ACTIVE_TOKEN_KEY_PREFIX = "queue:token:";
    private static final long ACTIVE_TOKEN_TTL_MINUTES = 5;

    @Override
    public QueueResponseDto enterQueue(QueueEnterRequestDto requestDto) {
        String token = UUID.randomUUID().toString();
        String waitingQueueKey = WAITING_QUEUE_KEY_PREFIX + requestDto.getProductId() + ":waiting";
        long score = System.currentTimeMillis();

        // TODO: [학습자 주도 영역] 논리 적용 필요 - 분산 락 / 비동기 큐 동시성 제어
        // 1. Redis ZSET (ZADD)에 사용자 토큰을 Score(현재 타임스탬프)로 등록
        // 2. 등록 후 현재 대기 순번(ZRANK) 조회
        // 3. QueueResponseDto.waiting() 반환

        return QueueResponseDto.waiting(token, requestDto.getProductId(), requestDto.getUserId(), 1L, 0L);
    }

    @Override
    public QueueResponseDto getQueueStatus(String token, Long productId) {
        String activeTokenKey = ACTIVE_TOKEN_KEY_PREFIX + token;
        String waitingQueueKey = WAITING_QUEUE_KEY_PREFIX + productId + ":waiting";

        // TODO: [학습자 주도 영역] 논리 적용 필요 - 분산 락 / 비동기 큐 동시성 제어
        // 1. 활성 토큰(String) 키 존재 여부 확인 -> 존재 시 ACTIVE 반환
        // 2. 대기열(ZSET)에서 해당 토큰의 순번(ZRANK) 조회 -> 존재 시 WAITING 및 잔여 순번 반환
        // 3. 둘 다 없으면 EXPIRED 반환

        return QueueResponseDto.expired(token);
    }

    @Override
    public long activateTokens(Long productId, long count) {
        String waitingQueueKey = WAITING_QUEUE_KEY_PREFIX + productId + ":waiting";

        // TODO: [학습자 주도 영역] 논리 적용 필요 - 분산 락 / 비동기 큐 동시성 제어
        // 1. ZSET에서 가장 오래 대기한 토큰 count개를 ZRANGE로 조회
        // 2. 조회된 토큰들을 Redis String 키(queue:token:{token})로 저장하고 TTL 5분 부여
        // 3. ZSET에서 승급된 토큰들을 ZREM으로 제거하여 원자적 상태 전이 보장

        return 0L;
    }

    @Override
    public boolean validateActiveToken(String token, Long productId) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String activeTokenKey = ACTIVE_TOKEN_KEY_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(activeTokenKey);
        return Boolean.TRUE.equals(exists);
    }
}
