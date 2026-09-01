package com.weverse.ticketing.domain.queue.service;

import com.weverse.ticketing.domain.queue.dto.QueueEnterRequestDto;
import com.weverse.ticketing.domain.queue.dto.QueueResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
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

    /**
     * 1. 선착순 대기열 진입 (POST /api/v1/queue/enter)
     * - UUID 기반 토큰 생성
     * - Redis Sorted Set(ZSET)에 현재 시간(Score)으로 등록하여 순서 보장 (ZADD)
     * - 나의 현재 대기 순번 조회 (ZRANK)
     */
    @Override
    public QueueResponseDto enterQueue(QueueEnterRequestDto requestDto) {
        String token = UUID.randomUUID().toString();
        String waitingQueueKey = WAITING_QUEUE_KEY_PREFIX + requestDto.getProductId() + ":waiting";
        long score = System.currentTimeMillis();

        // 1) Redis ZSET에 [Key: waitingQueueKey, Value: token, Score: 타임스탬프] 추가 (ZADD)
        redisTemplate.opsForZSet().add(waitingQueueKey, token, (double) score);

        // 2) 등록된 토큰의 순위(Rank, 0부터 시작) 조회 (ZRANK)
        Long rank = redisTemplate.opsForZSet().rank(waitingQueueKey, token);

        // 3) NullPointerException 방어 및 순번 계산
        // rank가 0이면 내 대기 순번은 1번째(waitingPosition = 1), 내 앞 대기자는 0명(aheadCount = 0)
        long aheadCount = (rank != null) ? rank : 0L;
        long waitingPosition = aheadCount + 1;

        log.info("[Queue Enter] ProductId: {}, UserId: {}, Token: {}, Rank: {}",
                requestDto.getProductId(), requestDto.getUserId(), token, waitingPosition);

        return QueueResponseDto.waiting(token, requestDto.getProductId(), requestDto.getUserId(), waitingPosition, aheadCount);
    }

    /**
     * 2. 대기열 실시간 상태 및 순번 폴링 (GET /api/v1/queue/status)
     * - 1순위: 활성 토큰(ACTIVE) 키가 존재하는지 확인 (String Key)
     * - 2순위: 아직 대기 중(WAITING)인지 ZSET 순번 조회 (ZRANK)
     * - 3순위: 둘 다 없으면 만료되었거나 비정상 토큰 (EXPIRED)
     */
    @Override
    public QueueResponseDto getQueueStatus(String token, Long productId) {
        if (token == null || token.isBlank()) {
            return QueueResponseDto.expired(token);
        }

        String activeTokenKey = ACTIVE_TOKEN_KEY_PREFIX + token;
        String waitingQueueKey = WAITING_QUEUE_KEY_PREFIX + productId + ":waiting";

        // Step 1: 이미 활성화(ACTIVE) 승급된 토큰인지 확인
        Boolean isActive = redisTemplate.hasKey(activeTokenKey);
        if (Boolean.TRUE.equals(isActive)) {
            return QueueResponseDto.active(token, productId, null);
        }

        // Step 2: 대기열(ZSET)에 아직 남아있는지 순번 조회
        Long rank = redisTemplate.opsForZSet().rank(waitingQueueKey, token);
        if (rank != null) {
            long aheadCount = rank;
            long waitingPosition = aheadCount + 1;
            return QueueResponseDto.waiting(token, productId, null, waitingPosition, aheadCount);
        }

        // Step 3: 대기열에도 없고 활성 토큰도 없으면 만료 처리
        return QueueResponseDto.expired(token);
    }

    /**
     * 3. 백그라운드 스케줄러를 통한 대기자 활성화 승급 (Active Token 발급)
     * - 대기열에서 가장 오래 기다린 상위 N명을 추출 (ZRANGE 0 ~ count-1)
     * - 추출된 토큰들에게 5분의 유효시간(TTL)을 부여하여 Redis String으로 저장
     * - 대기열(ZSET)에서 제거 (ZREM)하여 원자적 승급 완료
     */
    @Override
    public long activateTokens(Long productId, long count) {
        if (count <= 0) return 0L;

        String waitingQueueKey = WAITING_QUEUE_KEY_PREFIX + productId + ":waiting";

        // 1) 대기열에서 가장 오래 기다린 count명 추출 (0번 index부터 count-1번 index까지)
        Set<String> tokensToActivate = redisTemplate.opsForZSet().range(waitingQueueKey, 0, count - 1);

        if (tokensToActivate == null || tokensToActivate.isEmpty()) {
            return 0L;
        }

        // 2) 추출된 토큰들을 활성 토큰(String Key)으로 등록하고 TTL 5분 부여
        for (String token : tokensToActivate) {
            String activeTokenKey = ACTIVE_TOKEN_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(activeTokenKey, "ACTIVE", ACTIVE_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        }

        // 3) 대기열 ZSET에서 승급된 토큰들을 삭제
        redisTemplate.opsForZSet().remove(waitingQueueKey, tokensToActivate.toArray());

        log.info("[Queue Activate] ProductId: {}, Activated Count: {}", productId, tokensToActivate.size());
        return tokensToActivate.size();
    }

    /**
     * 4. 상품 상세/주문 요청 시 활성 토큰 유효성 검증
     */
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
