package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.OrderRequest;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    public Order createOrder(User user, OrderRequest request) {

        List<Cart> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        double total = 0;

        Order order = Order.builder()
                .user(user)
                .totalPrice(0)
                .status("PENDING")
                .shippingAddress(request.getShippingAddress())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();


        for (Cart cart : cartItems) {

            double price = cart.getProduct().getPrice();
            int qty = cart.getQuantity();

            total += qty * price;

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(cart.getProduct())
                    .quantity(qty)
                    .price(price)
                    .build();

            orderItems.add(item);
        }


        order.setItems(orderItems);
        order.setTotalPrice(total);


        return orderRepository.save(order);
    }

    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUser(user);
    }
}
