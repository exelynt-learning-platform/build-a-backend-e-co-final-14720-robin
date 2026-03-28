package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CartRequest;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.exception.InsufficientStockException;
import com.ecommerce.backend.exception.InvalidRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Cart addToCart(User user, CartRequest request) {
        if (user == null) {
            throw new InvalidRequestException("User must not be null");
        }

        if (request == null || request.getQuantity() <= 0) {
            throw new InvalidRequestException("Invalid cart request quantity");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
        }

        // Check if cart item already exists for this user and product
        Optional<Cart> existingCart = cartRepository.findByUserAndProductId(user, request.getProductId());

        if (existingCart.isPresent()) {
            // Update existing cart item quantity
            Cart cart = existingCart.get();
            int newQuantity = cart.getQuantity() + request.getQuantity();

            if (product.getStock() < newQuantity) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }

            cart.setQuantity(newQuantity);
            return cartRepository.save(cart);
        } else {
            // Create new cart item
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setProduct(product);
            cart.setQuantity(request.getQuantity());
            return cartRepository.save(cart);
        }
    }

    public List<Cart> getUserCart(User user) {
        return cartRepository.findByUser(user);
    }


    @Transactional
    public void removeFromCart(Long cartId, User user) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (!cart.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only remove items from your own cart");
        }

        cartRepository.delete(cart);
    }
}






