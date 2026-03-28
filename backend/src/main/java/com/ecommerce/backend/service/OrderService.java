package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.OrderItemResponse;
import com.ecommerce.backend.dto.OrderRequest;
import com.ecommerce.backend.dto.OrderResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.InsufficientStockException;
import com.ecommerce.backend.exception.InvalidRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Order createOrder(User user, OrderRequest request) {

        List<Cart> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new InvalidRequestException("Cart is empty");
        }

        // Calculate total first using BigDecimal for currency precision.
        // Products are fetched with a PESSIMISTIC_WRITE lock to prevent concurrent
        // stock deductions from resulting in negative stock values.
        BigDecimal total = BigDecimal.ZERO;
        for (Cart cart : cartItems) {
            Product product = productRepository.findByIdWithLock(cart.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + cart.getProduct().getId()));

            if (cart.getQuantity() <= 0) {
                throw new InvalidRequestException("Invalid quantity in cart: " + cart.getQuantity());
            }

            if (product.getStock() < cart.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStock() + ", Requested: " + cart.getQuantity());
            }

            BigDecimal itemTotal = BigDecimal.valueOf(product.getPrice())
                    .multiply(BigDecimal.valueOf(cart.getQuantity()));
            total = total.add(itemTotal);
        }

        // Round to 2 decimal places for currency
        total = total.setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .user(user)
                .totalPrice(total.doubleValue())
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();

        // Second pass: decrement stock and build order items (all under the same lock)
        for (Cart cart : cartItems) {
            Product product = productRepository.findByIdWithLock(cart.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + cart.getProduct().getId()));

            int qty = cart.getQuantity();
            BigDecimal price = BigDecimal.valueOf(product.getPrice()).setScale(2, RoundingMode.HALF_UP);

            // Re-check stock under lock before committing the deduction
            if (product.getStock() < qty) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStock() + ", Requested: " + qty);
            }

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(qty)
                    .price(price.doubleValue())
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

    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(orderStatus);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid order status: " + status +
                ". Valid statuses are: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED");
        }

        return orderRepository.save(order);
    }

    public List<OrderResponse> getUserOrders(User user) {
        return orderRepository.findByUser(user).stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    public OrderResponse convertToOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getShippingAddress(),
                items);
    }
}

