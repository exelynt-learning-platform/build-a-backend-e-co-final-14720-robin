package com.ecommerce.backend.dto;

import lombok.Data;

@Data
public class CartRequest {

    private Long productId;
    private int quantity;
}
