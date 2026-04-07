package com.sweetbook.server.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.domain.AlbumProjectStatus;
import com.sweetbook.server.album.domain.BookGenerationStatus;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.order.domain.Order;
import com.sweetbook.server.order.domain.OrderStatus;
import com.sweetbook.server.order.dto.CreateOrderApiRequest;
import com.sweetbook.server.order.dto.CreateOrderApiResponse;
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
    void createOrder_success_persistsCreatedOrder() {
        User user = userRepository.save(newUser("order-ok@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_test_001"));
        when(sweetbookOrdersClient.createOrder(anyMap(), anyString())).thenReturn("or_001");

        CreateOrderApiResponse response = orderService.createOrder(user.getId(), album.getId(), sampleRequest(null));

        assertThat(response.orderUid()).isEqualTo("or_001");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        Order saved = orderRepository.findById(response.orderId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CREATED);
        verify(sweetbookOrdersClient, times(1))
                .createOrder(anyMap(), eq("order-" + album.getId() + "-" + response.externalRef()));
    }

    @Test
    void createOrder_samePayload_returnsExistingOrder() {
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
    void createOrder_fails_whenBookNotGenerated() {
        User user = userRepository.save(newUser("order-invalid@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newDraftAlbum(user));

        assertThatThrownBy(() -> orderService.createOrder(user.getId(), album.getId(), sampleRequest(null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void createOrder_remoteFailure_marksOrderFailed() {
        User user = userRepository.save(newUser("order-failed@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newGeneratedAlbum(user, "bk_test_003"));
        when(sweetbookOrdersClient.createOrder(anyMap(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.SWEETBOOK_CALL_FAILED));

        assertThatThrownBy(() -> orderService.createOrder(user.getId(), album.getId(), sampleRequest("ext-fail-1")))
                .isInstanceOf(BusinessException.class);

        Order failed = orderRepository.findByAlbumProjectIdAndExternalRef(album.getId(), "ext-fail-1").orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(OrderStatus.FAILED);
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
                        "Tester",
                        "010-1234-5678",
                        "06101",
                        "Seoul Address 123",
                        "401",
                        "memo"
                ),
                externalRef,
                null
        );
    }
}
