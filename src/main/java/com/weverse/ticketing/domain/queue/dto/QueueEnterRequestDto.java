package com.weverse.ticketing.domain.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEnterRequestDto {
    private Long userId;
    private Long productId;
}
