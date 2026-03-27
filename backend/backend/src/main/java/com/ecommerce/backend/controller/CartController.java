package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CartRequest;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<Cart> addToCart(@AuthenticationPrincipal User user,
                                          @RequestBody CartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(user, request));
    }

    @GetMapping
    public ResponseEntity<List<Cart>> getCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cartService.getUserCart(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> remove(@PathVariable Long id,
                                         @AuthenticationPrincipal User user) {
        cartService.removeFromCart(id, user);
        return ResponseEntity.ok("Removed from cart");
    }
}