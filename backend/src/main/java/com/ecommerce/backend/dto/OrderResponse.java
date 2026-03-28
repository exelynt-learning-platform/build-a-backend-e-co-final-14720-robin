package com.ecommerce.backend.dto;

import com.ecommerce.backend.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private double totalPrice;
    private OrderStatus status;
    private String shippingAddress;
    private List<OrderItemResponse> items;
}