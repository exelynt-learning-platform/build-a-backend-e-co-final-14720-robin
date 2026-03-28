package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.OrderRequest;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        // Calculate total first
        double total = 0;
        for (Cart cart : cartItems) {
            Product product = cart.getProduct();
            if (product == null) {
                throw new RuntimeException("Invalid cart entry with missing product");
            }

            int qty = cart.getQuantity();
            if (qty <= 0) {
                throw new RuntimeException("Invalid quantity in cart");
            }

            if (product.getStock() < qty) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            total += qty * product.getPrice();
        }

        // Build order with correct total
        Order order = Order.builder()
                .user(user)
                .totalPrice(total)
                .status("PENDING")
                .shippingAddress(request.getShippingAddress())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();

        // Create order items and update stock
        for (Cart cart : cartItems) {
            Product product = cart.getProduct();
            int qty = cart.getQuantity();
            double price = product.getPrice();

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(qty)
                    .price(price)
                    .build();

            orderItems.add(item);

            product.setStock(product.getStock() - qty);
            productRepository.save(product);
        }

        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        cartRepository.deleteAll(cartItems);

        return savedOrder;
    }

    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Validate status
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }

        order.setStatus(status);
        return orderRepository.save(order);
    }

    private boolean isValidStatus(String status) {
        return status != null && (
            status.equals("PENDING") ||
            status.equals("PROCESSING") ||
            status.equals("SHIPPED") ||
            status.equals("DELIVERED") ||
            status.equals("CANCELLED")
        );
    }

    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUser(user);
    }
}
