package com.weverse.ticketing.domain.order.controller;

import com.weverse.ticketing.domain.order.dto.CreateOrderRequestDto;
import com.weverse.ticketing.domain.order.dto.CreateOrderResponseDto;
import com.weverse.ticketing.domain.order.service.OrderCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService orderCommandService;

    @PostMapping
    public ResponseEntity<CreateOrderResponseDto> createOrder(@Valid @RequestBody CreateOrderRequestDto requestDto) {
        CreateOrderResponseDto response = orderCommandService.createOrderAsync(requestDto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
