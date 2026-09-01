package com.weverse.ticketing.domain.queue.service;

import com.weverse.ticketing.domain.queue.dto.QueueEnterRequestDto;
import com.weverse.ticketing.domain.queue.dto.QueueResponseDto;

public interface QueueService {
    QueueResponseDto enterQueue(QueueEnterRequestDto requestDto);
    QueueResponseDto getQueueStatus(String token, Long productId);
    long activateTokens(Long productId, long count);
    boolean validateActiveToken(String token, Long productId);
}
