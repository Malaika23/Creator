package com.ecommerce.productservice.controller;

import com.ecommerce.common.context.UserContext;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.UnauthorizedException;
import com.ecommerce.productservice.dto.ProductRequestDto;
import com.ecommerce.productservice.dto.ProductResponseDto;
import com.ecommerce.productservice.dto.ReviewDto;
import com.ecommerce.productservice.model.Category;
import com.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto request) {
        verifyAdminOrSeller();
        ProductResponseDto product = productService.createProduct(request);
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable UUID id) {
        ProductResponseDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequestDto request) {
        verifyAdminOrSeller();
        ProductResponseDto product = productService.updateProduct(id, request);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        verifyAdmin();
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponseDto> products;
        if (search != null && !search.isBlank()) {
            products = productService.searchProducts(search, pageable);
        } else if (categoryId != null) {
            products = productService.getProductsByCategory(categoryId, pageable);
        } else {
            products = productService.getAllProducts(pageable);
        }
        return ResponseEntity.ok(products);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewDto> addReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewDto reviewDto) {
        UUID userId = getAuthenticatedUserId();
        ReviewDto review = productService.addReview(id, userId, reviewDto);
        return new ResponseEntity<>(review, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable UUID id) {
        List<ReviewDto> reviews = productService.getReviewsByProduct(id);
        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/categories")
    public ResponseEntity<Category> createCategory(@RequestParam String name) {
        verifyAdmin();
        Category category = productService.createCategory(name);
        return new ResponseEntity<>(category, HttpStatus.CREATED);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    private void verifyAdmin() {
        String role = UserContext.getCurrentContext().getRole();
        if (role == null || !role.equalsIgnoreCase("ADMIN")) {
            throw new UnauthorizedException("Access denied: Admin role required");
        }
    }

    private void verifyAdminOrSeller() {
        String role = UserContext.getCurrentContext().getRole();
        if (role == null || (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("SELLER"))) {
            throw new UnauthorizedException("Access denied: Admin or Seller role required");
        }
    }

    private UUID getAuthenticatedUserId() {
        String userIdStr = UserContext.getCurrentContext().getUserId();
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new UnauthorizedException("Authentication context missing");
        }
        try {
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid User ID format");
        }
    }
}
