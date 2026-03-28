package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.OrderRequest;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final com.ecommerce.backend.service.UserService userService;

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            @RequestBody OrderRequest request) {

        User user = userService.findByUsername(userDetails.getUsername());
        return ResponseEntity.ok(orderService.createOrder(user, request));
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {

        User user = userService.findByUsername(userDetails.getUsername());
        return ResponseEntity.ok(orderService.getUserOrders(user));
    }
}
