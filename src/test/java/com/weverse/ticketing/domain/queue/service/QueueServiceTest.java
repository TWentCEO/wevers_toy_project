package com.weverse.ticketing.domain.queue.service;

import com.weverse.ticketing.domain.queue.dto.QueueEnterRequestDto;
import com.weverse.ticketing.domain.queue.dto.QueueResponseDto;
import com.weverse.ticketing.domain.queue.dto.QueueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QueueServiceTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Long TEST_PRODUCT_ID = 100L;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 대기열 및 토큰 데이터 초기화
        Set<String> keys = redisTemplate.keys("queue:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("선착순 대기열 진입 시 순서대로 1번, 2번, 3번 순번이 부여되어야 한다.")
    void enterQueue_FIFO_Order_Success() throws InterruptedException {
        // Given: 3명의 사용자가 순차적으로 진입
        QueueEnterRequestDto user1 = new QueueEnterRequestDto(1L, TEST_PRODUCT_ID);
        QueueEnterRequestDto user2 = new QueueEnterRequestDto(2L, TEST_PRODUCT_ID);
        QueueEnterRequestDto user3 = new QueueEnterRequestDto(3L, TEST_PRODUCT_ID);

        // When
        QueueResponseDto res1 = queueService.enterQueue(user1);
        Thread.sleep(10); // 타임스탬프 간격
        QueueResponseDto res2 = queueService.enterQueue(user2);
        Thread.sleep(10);
        QueueResponseDto res3 = queueService.enterQueue(user3);

        // Then
        assertThat(res1.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(res1.getWaitingNumber()).isEqualTo(1L); // 1번째 대기자
        assertThat(res1.getRemainingWaitCount()).isEqualTo(0L); // 내 앞 대기자 0명

        assertThat(res2.getWaitingNumber()).isEqualTo(2L); // 2번째 대기자
        assertThat(res2.getRemainingWaitCount()).isEqualTo(1L); // 내 앞 대기자 1명

        assertThat(res3.getWaitingNumber()).isEqualTo(3L); // 3번째 대기자
        assertThat(res3.getRemainingWaitCount()).isEqualTo(2L); // 내 앞 대기자 2명
    }

    @Test
    @DisplayName("스케줄러가 상위 2명을 활성화하면 상태가 ACTIVE로 변경되고 대기열에서 빠져야 한다.")
    void activateTokens_Success() throws InterruptedException {
        // Given: 3명 대기열 진입
        QueueResponseDto res1 = queueService.enterQueue(new QueueEnterRequestDto(1L, TEST_PRODUCT_ID));
        Thread.sleep(10);
        QueueResponseDto res2 = queueService.enterQueue(new QueueEnterRequestDto(2L, TEST_PRODUCT_ID));
        Thread.sleep(10);
        QueueResponseDto res3 = queueService.enterQueue(new QueueEnterRequestDto(3L, TEST_PRODUCT_ID));

        // When: 상위 2명 활성화 승급
        long activatedCount = queueService.activateTokens(TEST_PRODUCT_ID, 2);

        // Then: 2명이 활성화됨
        assertThat(activatedCount).isEqualTo(2L);

        // 유저 1, 2는 ACTIVE 상태
        QueueResponseDto status1 = queueService.getQueueStatus(res1.getToken(), TEST_PRODUCT_ID);
        QueueResponseDto status2 = queueService.getQueueStatus(res2.getToken(), TEST_PRODUCT_ID);
        assertThat(status1.getStatus()).isEqualTo(QueueStatus.ACTIVE);
        assertThat(status2.getStatus()).isEqualTo(QueueStatus.ACTIVE);

        // 유저 3은 여전히 대기 중이지만 이제 1번 순번으로 당겨짐
        QueueResponseDto status3 = queueService.getQueueStatus(res3.getToken(), TEST_PRODUCT_ID);
        assertThat(status3.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(status3.getWaitingNumber()).isEqualTo(1L); // 앞선 2명이 빠져서 1번이 됨!
        assertThat(status3.getRemainingWaitCount()).isEqualTo(0L);
    }
}
