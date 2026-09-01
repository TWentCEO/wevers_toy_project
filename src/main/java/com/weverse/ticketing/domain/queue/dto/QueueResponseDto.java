package com.weverse.ticketing.domain.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueResponseDto {
    private String token;
    private Long productId;
    private Long userId;
    private QueueStatus status;
    private Long waitingNumber;
    private Long remainingWaitCount;

    public static QueueResponseDto waiting(String token, Long productId, Long userId, Long waitingNumber, Long remainingWaitCount) {
        return QueueResponseDto.builder()
                .token(token)
                .productId(productId)
                .userId(userId)
                .status(QueueStatus.WAITING)
                .waitingNumber(waitingNumber)
                .remainingWaitCount(remainingWaitCount)
                .build();
    }

    public static QueueResponseDto active(String token, Long productId, Long userId) {
        return QueueResponseDto.builder()
                .token(token)
                .productId(productId)
                .userId(userId)
                .status(QueueStatus.ACTIVE)
                .waitingNumber(0L)
                .remainingWaitCount(0L)
                .build();
    }

    public static QueueResponseDto expired(String token) {
        return QueueResponseDto.builder()
                .token(token)
                .status(QueueStatus.EXPIRED)
                .waitingNumber(0L)
                .remainingWaitCount(0L)
                .build();
    }
}
