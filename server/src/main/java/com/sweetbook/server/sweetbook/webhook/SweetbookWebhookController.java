package com.sweetbook.server.sweetbook.webhook;

import com.sweetbook.server.common.response.ApiResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks/sweetbook")
public class SweetbookWebhookController {

    private final SweetbookWebhookService sweetbookWebhookService;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receiveOrderWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Webhook-Event", required = false) String eventType,
            @RequestHeader(value = "X-Webhook-Delivery", required = false) String deliveryId,
            @RequestBody String rawBody
    ) {
        sweetbookWebhookService.handleOrderWebhook(signature, timestamp, eventType, deliveryId, rawBody);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("received", true)));
    }
}

