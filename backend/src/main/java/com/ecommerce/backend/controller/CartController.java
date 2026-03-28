package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.CartRequest;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.service.CartService;
import com.ecommerce.backend.service.UserService;
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
    private final UserService userService;

    @PostMapping("/add")
    public ResponseEntity<Cart> addToCart(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            @RequestBody CartRequest request) {

        User user = userService.findByUsername(userDetails.getUsername());

        return ResponseEntity.ok(cartService.addToCart(user, request));
    }

    @GetMapping
    public ResponseEntity<List<Cart>> getCart(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {

        User user = userService.findByUsername(userDetails.getUsername());

        return ResponseEntity.ok(cartService.getUserCart(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> remove(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {

        User user = userService.findByUsername(userDetails.getUsername());

        cartService.removeFromCart(id, user);

        return ResponseEntity.ok("Removed from cart");
    }
}