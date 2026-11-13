package com.yuxiang.commuity.controller;
import org.springframework.validation.annotation.Validated;
import com.yuxiang.commuity.dto.PageResponse;
import jakarta.validation.ConstraintViolationException;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.yuxiang.commuity.Product;
import com.yuxiang.commuity.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            errors.put(field, violation.getMessage());
        });

        return ResponseEntity.badRequest().body(errors);
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @PostMapping
    public Product createProduct(@Valid @RequestBody Product product) {
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @Valid @RequestBody Product updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "商品不存在"));

        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        product.setStatus(updatedProduct.getStatus());

        return productRepository.save(product);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在");
        }
        productRepository.deleteById(id);
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "商品不存在"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResponseStatusException ex) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("error", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @GetMapping("/page")
    public PageResponse<Product> getProductsPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") String name) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").descending()
        );

        String keyword = name.trim();

        Page<Product> result;

        if (keyword.isEmpty()) {
            result = productRepository.findAll(pageable);
        } else {
            result = productRepository.findByNameContainingIgnoreCase(
                    keyword,
                    pageable
            );
        }

        return PageResponse.from(result);
    }



}
