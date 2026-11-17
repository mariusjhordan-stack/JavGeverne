package com.cronos.payment.controller;

import com.cronos.payment.service.PaymentService;
import com.cronos.payment.service.PaymentService.PaymentView;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public Page<PaymentView> list(@RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return paymentService.adminList(status, page, size);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return paymentService.stats();
    }
}
