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

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

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

        if (!cart.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        cartRepository.delete(cart);
    }
}






