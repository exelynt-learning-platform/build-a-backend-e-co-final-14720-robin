package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CartRequest;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public Cart addToCart(User user, CartRequest request) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null");
        }

        if (request == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Invalid cart request quantity");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }

        Cart cart = new Cart();
        cart.setUser(user);
        cart.setProduct(product);
        cart.setQuantity(request.getQuantity());

        return cartRepository.save(cart);
    }

    public List<Cart> getUserCart(User user) {
        return cartRepository.findByUser(user);
    }


    public void removeFromCart(Long cartId, User user) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart == null || cart.getUser() == null || user == null || !cart.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized or invalid cart");
        }

        cartRepository.delete(cart);
    }
}






