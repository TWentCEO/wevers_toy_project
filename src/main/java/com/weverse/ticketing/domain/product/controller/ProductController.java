package com.weverse.ticketing.domain.product.controller;

import com.weverse.ticketing.domain.product.dto.ProductResponseDto;
import com.weverse.ticketing.domain.product.service.ProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductQueryService productQueryService;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductDetail(@PathVariable Long productId) {
        ProductResponseDto response = productQueryService.getProductDetail(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}/stock")
    public ResponseEntity<Integer> getAvailableStock(@PathVariable Long productId) {
        Integer stock = productQueryService.getAvailableStock(productId);
        return ResponseEntity.ok(stock);
    }
}
