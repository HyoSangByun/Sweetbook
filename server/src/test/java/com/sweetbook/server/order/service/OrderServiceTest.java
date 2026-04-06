package com.sweetbook.server.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.domain.AlbumProjectStatus;
import com.sweetbook.server.album.domain.BookGenerationStatus;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.order.domain.Order;
import com.sweetbook.server.order.domain.OrderStatus;
import com.sweetbook.server.order.dto.CreateOrderApiRequest;
import com.sweetbook.server.order.dto.CreateOrderApiResponse;
import com.sweetbook.server.order.dto.OrderDetailResponse;
import com.sweetbook.server.order.dto.OrderSummaryResponse;
import com.sweetbook.server.order.repository.OrderRepository;
import com.sweetbook.server.sweetbook.client.SweetbookOrdersClient;
import com.sweetbook.server.user.domain.User;
import com.sweetbook.server.user.domain.UserRole;
import com.sweetbook.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class OrderServiceTest {

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
    void 주문_생성_성공시_CREATED로_저장된다() {
        User user = userRepository.save(newUser("order-ok@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_test_001"));
        when(sweetbookOrdersClient.createOrder(anyMap(), anyString())).thenReturn("or_001");

        CreateOrderApiResponse response = orderService.createOrder(user.getId(), album.getId(), sampleRequest(null));

        assertThat(response.orderUid()).isEqualTo("or_001");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        Order saved = orderRepository.findById(response.orderId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(saved.getExternalRef()).isNotBlank();
        assertThat(saved.getRequestPayload()).contains("\"items\"");
    }

    @Test
    void 동일_payload_재요청시_기존_주문을_반환한다() {
        User user = userRepository.save(newUser("order-idempotent@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_test_002"));
        when(sweetbookOrdersClient.createOrder(anyMap(), anyString())).thenReturn("or_same");

        CreateOrderApiResponse first = orderService.createOrder(user.getId(), album.getId(), sampleRequest(null));
        CreateOrderApiResponse second = orderService.createOrder(user.getId(), album.getId(), sampleRequest(null));

        assertThat(second.orderId()).isEqualTo(first.orderId());
        assertThat(second.orderUid()).isEqualTo("or_same");
        verify(sweetbookOrdersClient, times(1)).createOrder(anyMap(), anyString());
    }

    @Test
    void 책이_생성되지_않은_앨범은_주문할_수_없다() {
        User user = userRepository.save(newUser("order-invalid@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newDraftAlbum(user));

        assertThatThrownBy(() -> orderService.createOrder(user.getId(), album.getId(), sampleRequest(null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 외부호출_실패시_주문상태를_FAILED로_남긴다() {
        User user = userRepository.save(newUser("order-failed@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_test_003"));
        when(sweetbookOrdersClient.createOrder(anyMap(), anyString()))
                .thenThrow(new BusinessException(com.sweetbook.server.common.exception.ErrorCode.SWEETBOOK_CALL_FAILED));

        assertThatThrownBy(() -> orderService.createOrder(user.getId(), album.getId(), sampleRequest("ext-fail-1")))
                .isInstanceOf(BusinessException.class);

        Order failed = orderRepository.findByAlbumProjectIdAndExternalRef(album.getId(), "ext-fail-1").orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(OrderStatus.FAILED);
    }

    @Test
    void 주문_목록과_상세_조회가_동작한다() {
        User user = userRepository.save(newUser("order-list@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_test_004"));
        when(sweetbookOrdersClient.createOrder(anyMap(), anyString())).thenReturn("or_list");
        CreateOrderApiResponse created = orderService.createOrder(user.getId(), album.getId(), sampleRequest("ext-list-1"));

        List<OrderSummaryResponse> list = orderService.listOrders(user.getId(), album.getId());
        OrderDetailResponse detail = orderService.getOrder(user.getId(), album.getId(), created.orderId());

        assertThat(list).hasSize(1);
        assertThat(list.get(0).orderUid()).isEqualTo("or_list");
        assertThat(detail.externalRef()).isEqualTo("ext-list-1");
        assertThat(detail.payload()).containsKey("items");
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

    private AlbumProject newDraftAlbum(User user) {
        return AlbumProject.builder()
                .user(user)
                .month("2026-04")
                .title("draft-album")
                .subtitle("sub")
                .monthlyReview("review")
                .status(AlbumProjectStatus.DRAFT)
                .bookStatus(BookGenerationStatus.NOT_GENERATED)
                .build();
    }

    private CreateOrderApiRequest sampleRequest(String externalRef) {
        return new CreateOrderApiRequest(
                List.of(new CreateOrderApiRequest.Item("bk_abc123", 1)),
                new CreateOrderApiRequest.Shipping(
                        "홍길동",
                        "010-1234-5678",
                        "06101",
                        "서울시 강남구 테헤란로 123",
                        "4층 401호",
                        "부재시 경비실"
                ),
                externalRef,
                null
        );
    }
}

