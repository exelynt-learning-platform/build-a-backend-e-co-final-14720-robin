package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.Order;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.secret}")
    private String stripeSecret;

    @PostConstruct
    public void init() {
        if (stripeSecret == null || stripeSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Stripe secret is not configured. Set STRIPE_SECRET environment variable.");
        }
        Stripe.apiKey = stripeSecret.trim();
        logger.info("Stripe API key initialized successfully");
    }

    public String createPaymentSession(Order order) throws Exception {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }

        logger.info("Creating payment session for order ID: {}", order.getId());

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:3000/success")
                        .setCancelUrl("http://localhost:3000/cancel")
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("usd")
                                                        .setUnitAmount((long) (order.getTotalPrice() * 100))
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("Order Payment")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

        Session session = Session.create(params);
        logger.info("Payment session created for order ID: {}, URL: {}", order.getId(), session.getUrl());

        return session.getUrl();
    }
}
