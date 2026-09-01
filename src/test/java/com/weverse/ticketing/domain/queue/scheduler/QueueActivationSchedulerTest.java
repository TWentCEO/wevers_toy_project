package com.weverse.ticketing.domain.queue.scheduler;

import com.weverse.ticketing.domain.queue.dto.QueueEnterRequestDto;
import com.weverse.ticketing.domain.queue.dto.QueueResponseDto;
import com.weverse.ticketing.domain.queue.dto.QueueStatus;
import com.weverse.ticketing.domain.queue.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QueueActivationSchedulerTest {

    @Autowired
    private QueueActivationScheduler queueActivationScheduler;

    @Autowired
    private QueueService queueService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Long TEST_PRODUCT_ID = 200L;

    @BeforeEach
    void setUp() {
        Set<String> keys = redisTemplate.keys("queue:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("스케줄러가 실행되면 대기열에 쌓인 유저들이 자동으로 ACTIVE 승급되어야 한다.")
    void scheduler_Auto_Activation_Success() {
        // Given: 5명의 대기자 진입
        QueueResponseDto user1 = queueService.enterQueue(new QueueEnterRequestDto(1L, TEST_PRODUCT_ID));
        QueueResponseDto user2 = queueService.enterQueue(new QueueEnterRequestDto(2L, TEST_PRODUCT_ID));
        QueueResponseDto user3 = queueService.enterQueue(new QueueEnterRequestDto(3L, TEST_PRODUCT_ID));

        // When: 스케줄러 1회 수동 트리거 실행
        queueActivationScheduler.processQueueActivation();

        // Then: 3명 모두 ACTIVE 상태로 승급되었는지 확인
        QueueResponseDto status1 = queueService.getQueueStatus(user1.getToken(), TEST_PRODUCT_ID);
        QueueResponseDto status2 = queueService.getQueueStatus(user2.getToken(), TEST_PRODUCT_ID);
        QueueResponseDto status3 = queueService.getQueueStatus(user3.getToken(), TEST_PRODUCT_ID);

        assertThat(status1.getStatus()).isEqualTo(QueueStatus.ACTIVE);
        assertThat(status2.getStatus()).isEqualTo(QueueStatus.ACTIVE);
        assertThat(status3.getStatus()).isEqualTo(QueueStatus.ACTIVE);
    }
}
