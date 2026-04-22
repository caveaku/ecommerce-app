package com.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

public class OrderDto {

    @Data
    public static class CreateOrderRequest {
        @NotEmpty
        private List<OrderItemRequest> items;
        private String shippingAddress;
    }

    @Data
    public static class OrderItemRequest {
        @NotNull private Long productId;
        @Min(1)  private Integer quantity;
    }
}
