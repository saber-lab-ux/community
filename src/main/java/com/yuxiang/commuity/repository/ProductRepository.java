package com.yuxiang.commuity.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

import com.yuxiang.commuity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}