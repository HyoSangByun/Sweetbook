package com.sweetbook.server.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.domain.AlbumProjectStatus;
import com.sweetbook.server.album.domain.BookGenerationStatus;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.order.domain.Order;
import com.sweetbook.server.order.domain.OrderStatus;
import com.sweetbook.server.order.dto.CreateOrderApiRequest;
import com.sweetbook.server.order.dto.CreateOrderApiResponse;
import com.sweetbook.server.order.repository.OrderRepository;
import com.sweetbook.server.sweetbook.client.SweetbookOrdersClient;
import com.sweetbook.server.user.domain.User;
import com.sweetbook.server.user.domain.UserRole;
import com.sweetbook.server.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.sweetbook.webhook-secret=e2e-webhook-secret",
        "app.sweetbook.webhook-timestamp-tolerance=10m"
})
@AutoConfigureMockMvc
class OrderWebhookE2ETest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlbumProjectRepository albumProjectRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SweetbookOrdersClient sweetbookOrdersClient;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        albumProjectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 주문_생성후_webhook_수신시_상태가_배송완료로_갱신된다() throws Exception {
        User user = userRepository.save(newUser("e2e@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_e2e_001"));

        when(sweetbookOrdersClient.createOrder(anyMap())).thenReturn("or_e2e_001");
        CreateOrderApiResponse created = orderService.createOrder(user.getId(), album.getId(), sampleRequest("ext-e2e-1"));
        assertThat(created.status()).isEqualTo(OrderStatus.CREATED);

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String body = """
                {
                  "event": "shipping.delivered",
                  "orderUid": "or_e2e_001",
                  "status": "DELIVERED",
                  "deliveredAt": "2026-04-06T03:00:00Z",
                  "timestamp": "2026-04-06T03:00:00Z"
                }
                """;
        String signature = "sha256=" + hmacSha256Hex("e2e-webhook-secret", timestamp + "." + body);

        mockMvc.perform(post("/api/webhooks/sweetbook/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", signature)
                        .header("X-Webhook-Timestamp", timestamp)
                        .header("X-Webhook-Event", "shipping.delivered")
                        .header("X-Webhook-Delivery", "wh_e2e_001")
                        .content(body))
                .andExpect(status().isOk());

        Order updated = orderRepository.findById(created.orderId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(updated.getRemoteOrderStatusCode()).isEqualTo(70);
        assertThat(updated.getRemoteOrderStatusDisplay()).isEqualTo("DELIVERED");
    }

    private User newUser(String email) {
        return User.builder()
                .email(email)
                .password("encoded-password")
                .role(UserRole.USER)
                .build();
    }

    private AlbumProject newGeneratedAlbum(User user, String bookUid) {
        AlbumProject album = AlbumProject.builder()
                .user(user)
                .month("2026-04")
                .title("order-album")
                .subtitle("sub")
                .monthlyReview("review")
                .status(AlbumProjectStatus.DRAFT)
                .bookStatus(BookGenerationStatus.NOT_GENERATED)
                .build();
        album.markBookGenerated(bookUid, LocalDateTime.now());
        return album;
    }

    private CreateOrderApiRequest sampleRequest(String externalRef) {
        return new CreateOrderApiRequest(
                List.of(new CreateOrderApiRequest.Item("bk_abc123", 1)),
                new CreateOrderApiRequest.Shipping(
                        "홍길동",
                        "010-1234-5678",
                        "06101",
                        "서울특별시 강남구 테헤란로 123",
                        "4층 401호",
                        "부재시 경비실"
                ),
                externalRef,
                null
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
