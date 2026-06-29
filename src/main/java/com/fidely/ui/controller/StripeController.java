package com.fidely.ui.controller;

import com.fidely.domain.dto.request.stripe.CancelSubscriptionRequest;
import com.fidely.domain.dto.request.stripe.CreateClienteRequest;
import com.fidely.domain.dto.request.stripe.SubscribeRequest;
import com.fidely.domain.dto.response.stripe.StripeResponse;
import com.fidely.domain.service.StripeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/client")
    public ResponseEntity<StripeResponse> createClient(@RequestBody CreateClienteRequest req) throws StripeException {
        String customerId = stripeService.createClient(req.email(), req.nombre());
        return ResponseEntity.ok(new StripeResponse("Cliente creado correctamente", customerId));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<StripeResponse> subscribe(@RequestBody SubscribeRequest req) throws StripeException {
        String clientSecret = stripeService.subscribe(req.customerId(), req.paymentMethodId());
        return ResponseEntity.ok(new StripeResponse("Suscripción creada", clientSecret));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) throws StripeException {
        stripeService.processWebhook(payload, signature, webhookSecret);
        return ResponseEntity.ok("ok");
    }

    @DeleteMapping("/subscription")
    public ResponseEntity<StripeResponse> cancel(@RequestBody CancelSubscriptionRequest req) throws StripeException {
        stripeService.cancel(req.subscriptionId());
        return ResponseEntity.ok(new StripeResponse("Suscripción cancelada correctamente", null));
    }
}
