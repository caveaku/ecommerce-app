package com.ecommerce.controller;

import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {
    private final CategoryRepository repo;

    @GetMapping
    public List<Category> getAll() { return repo.findAll(); }
}
