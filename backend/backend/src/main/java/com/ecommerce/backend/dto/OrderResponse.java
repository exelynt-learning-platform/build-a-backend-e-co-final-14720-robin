package com.ecommerce.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderResponse {

    private Long orderId;
    private double totalPrice;
    private String status;
    private String shippingAddress;
    private List<CartResponse> items;
}