package com.younive.store.payments;

import com.younive.store.common.ErrorDto;
import com.younive.store.carts.CartEmptyException;
import com.younive.store.carts.CartNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    @PostMapping
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return checkoutService.checkout(request);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload
    ) {
        try {
            checkoutService.handleWebhookEvent(new WebhookRequest(headers, payload));
            return ResponseEntity.ok().build();
        } catch (PaymentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<?> handlePaymentException(){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDto("Error creating a checkout session"));
    }

    @ExceptionHandler({CartNotFoundException.class, CartEmptyException.class})
    public ResponseEntity<ErrorDto> handleException(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorDto(e.getMessage()));
    }
}
