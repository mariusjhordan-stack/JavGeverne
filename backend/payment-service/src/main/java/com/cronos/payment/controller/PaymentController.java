package com.cronos.payment.controller;

import com.cronos.payment.security.RequestUser;
import com.cronos.payment.service.PaymentService;
import com.cronos.payment.service.PaymentService.CheckoutResponse;
import com.cronos.payment.service.PaymentService.PaymentView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    public record CheckoutRequest(@NotNull UUID orderId) {}

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/preference")
    public CheckoutResponse startCheckout(@Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal RequestUser user) {
        return paymentService.startCheckout(request.orderId(), user);
    }

    /** Public by necessity; authenticated by Stripe's signature instead of a session. */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader(value = "stripe-signature", required = false) String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/order/{orderId}")
    public PaymentView getForOrder(@PathVariable UUID orderId, @AuthenticationPrincipal RequestUser user) {
        return paymentService.getForOrder(orderId, user);
    }

    @PostMapping("/order/{orderId}/sync")
    public PaymentView sync(@PathVariable UUID orderId, @AuthenticationPrincipal RequestUser user) {
        return paymentService.sync(orderId, user);
    }
}
