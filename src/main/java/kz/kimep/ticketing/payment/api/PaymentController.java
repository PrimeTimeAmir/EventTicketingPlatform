package kz.kimep.ticketing.payment.api;

import jakarta.validation.Valid;
import kz.kimep.ticketing.payment.api.dto.*;
import kz.kimep.ticketing.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Create payment
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        PaymentResponse response = paymentService.createPayment(
                request,
                userDetails.getUsername()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Simulate payment result (development/testing only)
    @PostMapping("/{transactionId}/simulate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentSimulationResponse> simulatePayment(
            @PathVariable String transactionId,
            @Valid @RequestBody PaymentSimulationRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        PaymentSimulationResponse response =
                paymentService.simulatePayment(
                        transactionId,
                        request,
                        userDetails.getUsername()
                );

        return ResponseEntity.ok(response);
    }
}