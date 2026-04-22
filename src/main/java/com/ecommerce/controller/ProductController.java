package com.ecommerce.controller;

import com.ecommerce.model.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductRepository  productRepo;
    private final CategoryRepository categoryRepo;

    @GetMapping
    public List<Product> getAll() { return productRepo.findByActiveTrue(); }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam String q) { return productRepo.search(q); }

    @GetMapping("/category/{categoryId}")
    public List<Product> byCategory(@PathVariable Long categoryId) {
        return productRepo.findByCategoryIdAndActiveTrue(categoryId);
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            categoryRepo.findById(product.getCategory().getId())
                    .ifPresent(product::setCategory);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productRepo.save(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id,
                                          @Valid @RequestBody Product updated) {
        return productRepo.findById(id).map(p -> {
            p.setName(updated.getName());
            p.setDescription(updated.getDescription());
            p.setPrice(updated.getPrice());
            p.setStockQuantity(updated.getStockQuantity());
            p.setImageUrl(updated.getImageUrl());
            p.setActive(updated.getActive());
            if (updated.getCategory() != null && updated.getCategory().getId() != null) {
                categoryRepo.findById(updated.getCategory().getId()).ifPresent(p::setCategory);
            }
            return ResponseEntity.ok(productRepo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return productRepo.findById(id).map(p -> {
            p.setActive(false); // soft delete
            productRepo.save(p);
            return ResponseEntity.<Void>noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
