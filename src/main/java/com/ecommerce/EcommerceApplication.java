package com.ecommerce;

import com.ecommerce.model.*;
import com.ecommerce.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Set;

@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }

    /** Seed demo data on startup */
    @Bean
    CommandLineRunner seedData(ProductRepository productRepo,
                               UserRepository userRepo,
                               CategoryRepository categoryRepo,
                               PasswordEncoder encoder) {
        return args -> {
            // Categories
            Category electronics = categoryRepo.save(new Category(null, "Electronics", "Electronic devices and accessories"));
            Category clothing    = categoryRepo.save(new Category(null, "Clothing",    "Fashion and apparel"));
            Category books       = categoryRepo.save(new Category(null, "Books",       "Books and literature"));

            // Products
            productRepo.save(new Product(null, "MacBook Pro 16\"", "Apple M3 Pro chip, 18GB RAM, 512GB SSD",
                    new BigDecimal("2499.99"), 15, "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400", electronics, true));
            productRepo.save(new Product(null, "Sony WH-1000XM5", "Industry-leading noise cancelling headphones",
                    new BigDecimal("349.99"), 42, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400", electronics, true));
            productRepo.save(new Product(null, "iPhone 15 Pro", "Titanium design, A17 Pro chip, 48MP camera",
                    new BigDecimal("1199.99"), 30, "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=400", electronics, true));
            productRepo.save(new Product(null, "Classic Oxford Shirt", "100% cotton, slim fit, multiple colors",
                    new BigDecimal("79.99"), 120, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=400", clothing, true));
            productRepo.save(new Product(null, "Slim Fit Chinos", "Stretch cotton, modern cut, versatile style",
                    new BigDecimal("59.99"), 85, "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=400", clothing, true));
            productRepo.save(new Product(null, "Clean Code", "A Handbook of Agile Software Craftsmanship by Robert C. Martin",
                    new BigDecimal("34.99"), 200, "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400", books, true));
            productRepo.save(new Product(null, "The Pragmatic Programmer", "Your Journey to Mastery, 20th Anniversary Edition",
                    new BigDecimal("49.99"), 150, "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=400", books, true));
            productRepo.save(new Product(null, "Samsung 4K Monitor 27\"", "144Hz, HDR, 1ms response time",
                    new BigDecimal("599.99"), 20, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=400", electronics, true));

            // Demo users
            if (userRepo.findByEmail("admin@shop.com").isEmpty()) {
                userRepo.save(new User(null, "Admin User", "admin@shop.com",
                        encoder.encode("admin123"), Role.ADMIN));
            }
            if (userRepo.findByEmail("user@shop.com").isEmpty()) {
                userRepo.save(new User(null, "John Doe", "user@shop.com",
                        encoder.encode("user123"), Role.USER));
            }

            System.out.println("✅ Seed data loaded successfully.");
        };
    }
}
