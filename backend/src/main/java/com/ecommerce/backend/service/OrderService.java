package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.OrderRequest;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public Order createOrder(User user, OrderRequest request) {

        List<Cart> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Calculate total first using BigDecimal for currency precision
        BigDecimal total = BigDecimal.ZERO;
        for (Cart cart : cartItems) {
            Product product = cart.getProduct();
            if (product == null) {
                throw new RuntimeException("Invalid cart entry with missing product");
            }

            int qty = cart.getQuantity();
            if (qty <= 0) {
                throw new RuntimeException("Invalid quantity in cart: " + qty);
            }

            if (product.getStock() < qty) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            BigDecimal itemTotal = BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(qty));
            total = total.add(itemTotal);
        }

        // Round to 2 decimal places for currency
        total = total.setScale(2, RoundingMode.HALF_UP);

        // Build order with correct total
        Order order = Order.builder()
                .user(user)
                .totalPrice(total.doubleValue())
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();

        // Create order items and update stock
        for (Cart cart : cartItems) {
            Product product = cart.getProduct();
            int qty = cart.getQuantity();
            BigDecimal price = BigDecimal.valueOf(product.getPrice()).setScale(2, RoundingMode.HALF_UP);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(qty)
                    .price(price.doubleValue())
                    .build();

            orderItems.add(item);

            // Update stock with optimistic locking
            try {
                product.setStock(product.getStock() - qty);
                productRepository.save(product);
            } catch (ObjectOptimisticLockingFailureException e) {
                throw new RuntimeException("Product " + product.getName() + " is currently being updated by another user. Please try again.");
            }
        }

        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        cartRepository.deleteAll(cartItems);

        return savedOrder;
    }

    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(orderStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + status +
                ". Valid statuses are: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED");
        }

        return orderRepository.save(order);
    }

    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUser(user);
    }
}
