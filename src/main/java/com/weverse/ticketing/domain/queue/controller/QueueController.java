package com.weverse.ticketing.domain.queue.controller;

import com.weverse.ticketing.domain.queue.dto.QueueEnterRequestDto;
import com.weverse.ticketing.domain.queue.dto.QueueResponseDto;
import com.weverse.ticketing.domain.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/enter")
    public ResponseEntity<QueueResponseDto> enterQueue(@RequestBody QueueEnterRequestDto requestDto) {
        QueueResponseDto response = queueService.enterQueue(requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<QueueResponseDto> getQueueStatus(
            @RequestHeader("Queue-Token") String token,
            @RequestParam("productId") Long productId
    ) {
        QueueResponseDto response = queueService.getQueueStatus(token, productId);
        return ResponseEntity.ok(response);
    }
}
