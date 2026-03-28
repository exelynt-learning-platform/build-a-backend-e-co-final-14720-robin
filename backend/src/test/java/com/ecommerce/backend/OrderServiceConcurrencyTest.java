package com.ecommerce.backend;

import com.ecommerce.backend.dto.OrderRequest;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .username("testuser")
                .password("password")
                .role(Role.ROLE_USER)
                .build();
        testUser = userRepository.save(testUser);

        // Create test product with limited stock
        testProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(10.0)
                .stock(2) // Limited stock to test concurrency
                .imageUrl("test.jpg")
                .build();
        testProduct = productRepository.save(testProduct);

        // Create cart item
        Cart cart = Cart.builder()
                .user(testUser)
                .product(testProduct)
                .quantity(2)
                .build();
        cartRepository.save(cart);
    }

    @Test
    void testConcurrentOrderCreation_HandlesOptimisticLocking() throws Exception {
        // Create two concurrent order requests
        OrderRequest request1 = new OrderRequest();
        request1.setShippingAddress("Address 1");

        OrderRequest request2 = new OrderRequest();
        request2.setShippingAddress("Address 2");

        // Run both orders concurrently
        CompletableFuture<Order> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                return orderService.createOrder(testUser, request1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Order> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                return orderService.createOrder(testUser, request2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // At least one should succeed, at least one should fail due to optimistic locking
        int successCount = 0;
        int failureCount = 0;

        try {
            Order order1 = future1.get();
            assertNotNull(order1);
            successCount++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException &&
                e.getCause().getMessage().contains("currently being updated")) {
                failureCount++;
            } else {
                fail("Unexpected exception: " + e.getCause().getMessage());
            }
        }

        try {
            Order order2 = future2.get();
            assertNotNull(order2);
            successCount++;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException &&
                e.getCause().getMessage().contains("currently being updated")) {
                failureCount++;
            } else {
                fail("Unexpected exception: " + e.getCause().getMessage());
            }
        }

        // Verify that exactly one order succeeded and one failed
        assertEquals(1, successCount, "Exactly one order should succeed");
        assertEquals(1, failureCount, "Exactly one order should fail due to optimistic locking");

        // Verify stock levels
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(0, updatedProduct.getStock(), "Stock should be depleted by successful order");

        // Verify only one order was created
        assertEquals(1, orderRepository.count(), "Only one order should exist in database");
    }

    @Test
    void testSequentialOrderCreation_WorksNormally() {
        OrderRequest request = new OrderRequest();
        request.setShippingAddress("Test Address");

        // First order should succeed
        Order order1 = orderService.createOrder(testUser, request);
        assertNotNull(order1);
        assertEquals(OrderStatus.PENDING, order1.getStatus());
        assertEquals(20.0, order1.getTotalPrice()); // 2 items * $10

        // Verify stock is depleted
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(0, updatedProduct.getStock());

        // Second order should fail due to insufficient stock
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(testUser, request);
        }, "Second order should fail due to insufficient stock");
    }
}