package com.sweetbook.server.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.domain.AlbumProjectStatus;
import com.sweetbook.server.album.domain.BookGenerationStatus;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.order.domain.Order;
import com.sweetbook.server.order.domain.OrderStatus;
import com.sweetbook.server.order.dto.CancelOrderApiRequest;
import com.sweetbook.server.order.dto.OrderDetailResponse;
import com.sweetbook.server.order.dto.UpdateOrderShippingRequest;
import com.sweetbook.server.order.repository.OrderRepository;
import com.sweetbook.server.sweetbook.client.SweetbookOrdersClient;
import com.sweetbook.server.user.domain.User;
import com.sweetbook.server.user.domain.UserRole;
import com.sweetbook.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OrderModificationServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlbumProjectRepository albumProjectRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private SweetbookOrdersClient sweetbookOrdersClient;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        albumProjectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 취소_API_성공시_상태가_CANCELLED로_변경된다() {
        User user = userRepository.save(newUser("cancel@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_cancel_001"));
        Order order = orderRepository.save(newOrder(album, "or_cancel_001", 20));

        when(sweetbookOrdersClient.cancelOrder(anyString(), anyString()))
                .thenReturn(Map.of("orderStatus", 80, "orderStatusDisplay", "CANCELLED"));

        OrderDetailResponse response = orderService.cancelOrder(
                user.getId(),
                album.getId(),
                order.getId(),
                new CancelOrderApiRequest("고객 요청")
        );

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.remoteOrderStatusCode()).isEqualTo(80);
    }

    @Test
    void 취소_불가_상태면_예외가_발생한다() {
        User user = userRepository.save(newUser("cancel-block@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_cancel_002"));
        Order order = orderRepository.save(newOrder(album, "or_cancel_002", 30));

        assertThatThrownBy(() -> orderService.cancelOrder(
                user.getId(),
                album.getId(),
                order.getId(),
                new CancelOrderApiRequest("고객 요청")
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void 배송지_변경_API_성공시_request_payload가_갱신된다() {
        User user = userRepository.save(newUser("shipping@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_ship_001"));
        Order order = orderRepository.save(newOrder(album, "or_ship_001", 25));

        when(sweetbookOrdersClient.updateShipping(anyString(), anyMap()))
                .thenReturn(Map.of("orderStatus", 25, "orderStatusDisplay", "PDF_READY"));

        OrderDetailResponse response = orderService.updateOrderShipping(
                user.getId(),
                album.getId(),
                order.getId(),
                new UpdateOrderShippingRequest(
                        "김영희",
                        null,
                        null,
                        "서울시 서초구 반포대로 100",
                        null,
                        null
                )
        );

        assertThat(response.payload()).containsKey("shipping");
        Object shippingRaw = response.payload().get("shipping");
        assertThat(shippingRaw).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> shipping = (Map<String, Object>) shippingRaw;
        assertThat(shipping.get("recipientName")).isEqualTo("김영희");
        assertThat(shipping.get("address1")).isEqualTo("서울시 서초구 반포대로 100");
    }

    private Order newOrder(AlbumProject album, String orderUid, Integer remoteCode) {
        Order order = Order.builder()
                .albumProject(album)
                .orderUid(orderUid)
                .externalRef("ext-" + orderUid)
                .requestPayload("{\"items\":[],\"shipping\":{\"recipientName\":\"홍길동\"}}")
                .status(OrderStatus.CREATED)
                .build();
        order.updateRemoteStatus(remoteCode, "STATUS", LocalDateTime.now());
        return order;
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
}

