package com.ecommerce.backend.controller;

import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.service.OrderService;
import com.ecommerce.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @PostMapping("/{orderId}")
    public ResponseEntity<String> createPayment(@PathVariable Long orderId) throws Exception {
        Order order = orderService.getOrderById(orderId);
        String paymentUrl = paymentService.createPaymentSession(order);
        return ResponseEntity.ok(paymentUrl);
    }
}
