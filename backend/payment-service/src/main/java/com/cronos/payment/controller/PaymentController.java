package com.cronos.payment.controller;

import com.cronos.payment.security.RequestUser;
import com.cronos.payment.service.PaymentService;
import com.cronos.payment.service.PaymentService.CheckoutResponse;
import com.cronos.payment.service.PaymentService.PaymentView;
import com.fasterxml.jackson.databind.JsonNode;
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

    /** Public by necessity (Mercado Pago calls it); authenticated by its HMAC signature instead of a session. */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestParam(name = "data.id", required = false) String dataId,
                                        @RequestParam(name = "type", required = false) String type,
                                        @RequestBody(required = false) JsonNode body,
                                        @RequestHeader(value = "x-signature", required = false) String signature,
                                        @RequestHeader(value = "x-request-id", required = false) String requestId) {
        String bodyType = body == null ? null : body.path("type").asText(null);
        String bodyDataId = body == null ? null : body.path("data").path("id").asText(null);
        paymentService.handleWebhook(dataId, type != null ? type : bodyType, bodyDataId, signature, requestId);
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
