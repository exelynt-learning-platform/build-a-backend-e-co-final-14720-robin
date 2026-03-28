package com.ecommerce.backend.service;

import com.ecommerce.backend.entity.Order;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Value("${stripe.secret}")
    private String stripeSecret;

    @PostConstruct
    public void init() {
        // Only initialize Stripe if secret is provided, otherwise use lazy initialization
        if (stripeSecret != null && !stripeSecret.trim().isEmpty()) {
            Stripe.apiKey = stripeSecret.trim();
        }
        // If not configured, Stripe will be initialized lazily on first use
    }

    private void ensureStripeInitialized() {
        if (Stripe.apiKey == null) {
            if (stripeSecret != null && !stripeSecret.trim().isEmpty()) {
                Stripe.apiKey = stripeSecret.trim();
            } else {
                throw new IllegalStateException("Stripe secret is not configured. Set stripe.secret via environment or properties.");
            }
        }
    }

    public String createPaymentSession(Order order) throws Exception {
        if (order == null) {
            throw new IllegalArgumentException("Order must not be null");
        }

        ensureStripeInitialized();

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

        return session.getUrl();
    }
}



