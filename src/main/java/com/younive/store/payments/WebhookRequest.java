package com.younive.store.payments;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class WebhookRequest {
    private Map<String, String> headers;
    private String Payload;
}
