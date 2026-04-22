package com.ecommerce.controller;

import com.ecommerce.dto.OrderDto.*;
import com.ecommerce.model.*;
import com.ecommerce.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository   orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository    userRepo;

    @GetMapping
    public List<Order> myOrders(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepo.findByEmail(principal.getUsername()).orElseThrow();
        return orderRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@Valid @RequestBody CreateOrderRequest req,
                                        @AuthenticationPrincipal UserDetails principal) {
        User user = userRepo.findByEmail(principal.getUsername()).orElseThrow();
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(req.getShippingAddress());

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : req.getItems()) {
            Product product = productRepo.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            if (product.getStockQuantity() < itemReq.getQuantity()) {
                return ResponseEntity.badRequest()
                        .body("Insufficient stock for: " + product.getName());
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            OrderItem item = new OrderItem(null, order, product, itemReq.getQuantity(), product.getPrice(), subtotal);
            items.add(item);
            total = total.add(subtotal);

            // Deduct stock
            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            productRepo.save(product);
        }

        order.setItems(items);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.CONFIRMED);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderRepo.save(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails principal) {
        return orderRepo.findById(id)
                .filter(o -> o.getUser().getEmail().equals(principal.getUsername()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
