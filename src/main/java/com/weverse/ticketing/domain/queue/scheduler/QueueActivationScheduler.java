package com.weverse.ticketing.domain.queue.scheduler;

import com.weverse.ticketing.domain.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * [대기열 자동 승급 스케줄러]
 * - 1초(fixedDelay = 1000)마다 백그라운드에서 실행
 * - 현재 대기열에 줄 서 있는 유저들 중 상위 N명을 자동으로 ACTIVE 상태로 승급시킴
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueueActivationScheduler {

    private final QueueService queueService;
    private final StringRedisTemplate redisTemplate;

    // 한 번에 문을 열어 입장시킬 인원 수 (초당 100명씩 입장)
    private static final long BATCH_ACTIVATE_SIZE = 100;

    /**
     * 1초마다 대기열을 감시하여 대기자를 순차적으로 입장시킵니다.
     */
    @Scheduled(fixedDelay = 1000)
    public void processQueueActivation() {
        // 1. 현재 대기열이 존재하는 상품들의 Redis 키 검색 (예: "queue:product:100:waiting")
        Set<String> queueKeys = redisTemplate.keys("queue:product:*:waiting");

        if (queueKeys == null || queueKeys.isEmpty()) {
            return; // 줄 서 있는 상품 대기열이 없으면 스케줄러 종료
        }

        // 2. 각 상품 대기열마다 맨 앞 100명씩 입장(ACTIVE 승급) 처리
        for (String queueKey : queueKeys) {
            try {
                // 키 이름에서 productId 추출 (예: "queue:product:100:waiting" -> 100L)
                Long productId = extractProductIdFromKey(queueKey);
                if (productId != null) {
                    long activatedCount = queueService.activateTokens(productId, BATCH_ACTIVATE_SIZE);
                    if (activatedCount > 0) {
                        log.info("[Scheduler] ProductId: {} 대기열에서 {}명을 ACTIVE로 승급 완료했습니다.", productId, activatedCount);
                    }
                }
            } catch (Exception e) {
                log.error("[Scheduler Error] 대기열 승급 처리 중 오류 발생: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * "queue:product:100:waiting" 문자열에서 "100"을 파싱하여 Long으로 변환하는 헬퍼 메서드
     */
    private Long extractProductIdFromKey(String key) {
        try {
            String[] parts = key.split(":");
            return Long.parseLong(parts[2]);
        } catch (Exception e) {
            log.warn("[Key Parse Warning] 올바르지 않은 대기열 키 형식: {}", key);
            return null;
        }
    }
}
