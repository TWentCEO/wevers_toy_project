package com.weverse.ticketing.domain.product.repository;

import com.weverse.ticketing.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
