package com.sweetbook.server.sweetbook.webhook;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sweetbook.server.order.service.OrderService;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.sweetbook.webhook-secret=test-webhook-secret"
})
@AutoConfigureMockMvc
class SweetbookWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void 유효한_서명이면_webhook을_수신하고_200을_반환한다() throws Exception {
        String timestamp = "1709280000";
        String body = "{\"data\":{\"orderUid\":\"or_abc123\",\"orderStatus\":30,\"orderStatusDisplay\":\"제작 확정\",\"orderedAt\":\"2026-04-06T01:10:47Z\"}}";
        String signature = "sha256=" + hmacSha256Hex("test-webhook-secret", timestamp + "." + body);

        mockMvc.perform(post("/api/webhooks/sweetbook/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", signature)
                        .header("X-Webhook-Timestamp", timestamp)
                        .header("X-Webhook-Event", "production.confirmed")
                        .header("X-Webhook-Delivery", "wh_abc123")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.received").value(true));

        verify(orderService).applyWebhookStatusUpdateByEvent(
                org.mockito.ArgumentMatchers.eq("or_abc123"),
                org.mockito.ArgumentMatchers.eq("production.confirmed"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("wh_abc123")
        );
    }

    @Test
    void 서명이_유효하지_않으면_401을_반환한다() throws Exception {
        String timestamp = "1709280000";
        String body = "{\"data\":{\"orderUid\":\"or_abc123\",\"orderStatus\":30}}";

        mockMvc.perform(post("/api/webhooks/sweetbook/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", "sha256=invalid")
                        .header("X-Webhook-Timestamp", timestamp)
                        .header("X-Webhook-Event", "production.confirmed")
                        .header("X-Webhook-Delivery", "wh_abc123")
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_001"));

        verify(orderService, never()).applyWebhookStatusUpdateByEvent(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private String hmacSha256Hex(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
