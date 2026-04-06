package com.sweetbook.server.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.domain.BookGenerationStatus;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.order.domain.Order;
import com.sweetbook.server.order.domain.OrderStatus;
import com.sweetbook.server.order.dto.CreateOrderApiRequest;
import com.sweetbook.server.order.dto.CreateOrderApiResponse;
import com.sweetbook.server.order.dto.OrderDetailResponse;
import com.sweetbook.server.order.dto.OrderSummaryResponse;
import com.sweetbook.server.order.repository.OrderRepository;
import com.sweetbook.server.sweetbook.client.SweetbookOrdersClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final ObjectMapper CANONICAL_OBJECT_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private final AlbumProjectRepository albumProjectRepository;
    private final OrderRepository orderRepository;
    private final SweetbookOrdersClient sweetbookOrdersClient;
    private final PlatformTransactionManager transactionManager;

    public CreateOrderApiResponse createOrder(Long userId, Long albumId, CreateOrderApiRequest request) {
        Map<String, Object> payload = buildBasePayload(request);
        String payloadJsonForRef = toCanonicalJson(payload);
        String externalRef = resolveExternalRef(albumId, request.externalRef(), payloadJsonForRef);
        payload.put("externalRef", externalRef);
        String payloadJson = toCanonicalJson(payload);
        String idempotencyKey = "order-" + externalRef;

        OrderPreparation preparation = prepareOrder(userId, albumId, externalRef, payloadJson);
        if (preparation.existingCreatedOrder() != null) {
            return toCreateResponse(preparation.existingCreatedOrder());
        }

        try {
            String orderUid = sweetbookOrdersClient.createOrder(payload, idempotencyKey);
            return markCreated(userId, albumId, externalRef, orderUid);
        } catch (BusinessException ex) {
            markFailed(userId, albumId, externalRef, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markFailed(userId, albumId, externalRef, ex.getMessage());
            throw ex;
        }
    }

    public List<OrderSummaryResponse> listOrders(Long userId, Long albumId) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        return orderRepository.findAllByAlbumProjectIdOrderByCreatedAtDesc(albumProject.getId()).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public OrderDetailResponse getOrder(Long userId, Long albumId, Long orderId) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        Order order = orderRepository.findByIdAndAlbumProjectId(orderId, albumProject.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "주문을 찾을 수 없습니다."));
        return toDetailResponse(order);
    }

    private OrderPreparation prepareOrder(Long userId, Long albumId, String externalRef, String payloadJson) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> {
            AlbumProject albumProject = getOwnedAlbum(userId, albumId);
            validateOrderableAlbum(albumProject);

            Order existing = orderRepository.findByAlbumProjectIdAndExternalRef(albumId, externalRef).orElse(null);
            if (existing != null && existing.getStatus() == OrderStatus.CREATED) {
                return new OrderPreparation(existing);
            }

            if (existing != null) {
                existing.markRequested(payloadJson);
                orderRepository.saveAndFlush(existing);
                return new OrderPreparation(null);
            }

            try {
                Order order = Order.builder()
                        .albumProject(albumProject)
                        .externalRef(externalRef)
                        .requestPayload(payloadJson)
                        .status(OrderStatus.REQUESTED)
                        .build();
                orderRepository.saveAndFlush(order);
            } catch (DataIntegrityViolationException ex) {
                Order conflicted = orderRepository.findByAlbumProjectIdAndExternalRef(albumId, externalRef)
                        .orElseThrow(() -> ex);
                if (conflicted.getStatus() == OrderStatus.CREATED) {
                    return new OrderPreparation(conflicted);
                }
                conflicted.markRequested(payloadJson);
                orderRepository.saveAndFlush(conflicted);
            }
            return new OrderPreparation(null);
        });
    }

    private CreateOrderApiResponse markCreated(Long userId, Long albumId, String externalRef, String orderUid) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> {
            getOwnedAlbum(userId, albumId);
            Order order = orderRepository.findByAlbumProjectIdAndExternalRef(albumId, externalRef)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INVALID_INPUT,
                            "주문 상태 레코드를 찾을 수 없습니다."
                    ));
            order.markCreated(orderUid);
            return toCreateResponse(order);
        });
    }

    private void markFailed(Long userId, Long albumId, String externalRef, String errorMessage) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            getOwnedAlbum(userId, albumId);
            orderRepository.findByAlbumProjectIdAndExternalRef(albumId, externalRef)
                    .ifPresent(order -> order.markFailed(trimError(errorMessage)));
        });
    }

    private AlbumProject getOwnedAlbum(Long userId, Long albumId) {
        return albumProjectRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
    }

    private void validateOrderableAlbum(AlbumProject albumProject) {
        if (albumProject.getBookStatus() != BookGenerationStatus.GENERATED
                || albumProject.getBookUid() == null
                || albumProject.getBookUid().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "책 생성이 완료된 앨범만 주문할 수 있습니다."
            );
        }
    }

    private Map<String, Object> buildBasePayload(CreateOrderApiRequest request) {
        List<Map<String, Object>> itemMaps = request.items().stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookUid", item.bookUid());
                    map.put("quantity", item.quantity());
                    return map;
                })
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", itemMaps);
        payload.put("shipping", request.shipping().toMap());
        if (request.externalUserId() != null && !request.externalUserId().isBlank()) {
            payload.put("externalUserId", request.externalUserId().trim());
        }
        return payload;
    }

    private String resolveExternalRef(Long albumId, String requestedExternalRef, String payloadJson) {
        if (requestedExternalRef != null && !requestedExternalRef.isBlank()) {
            return requestedExternalRef.trim();
        }
        byte[] hash = digestSha256(payloadJson.getBytes(StandardCharsets.UTF_8));
        return "album-" + albumId + "-order-" + HexFormat.of().formatHex(hash).substring(0, 24);
    }

    private String toCanonicalJson(Map<String, Object> payload) {
        try {
            return CANONICAL_OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            BusinessException be = new BusinessException(ErrorCode.INVALID_INPUT, "주문 payload 직렬화에 실패했습니다.");
            be.initCause(e);
            throw be;
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return CANONICAL_OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return Map.of("raw", json);
        }
    }

    private byte[] digestSha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            BusinessException be = new BusinessException(ErrorCode.INTERNAL_ERROR, "해시 알고리즘을 사용할 수 없습니다.");
            be.initCause(e);
            throw be;
        }
    }

    private String trimError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() <= 500 ? errorMessage : errorMessage.substring(0, 500);
    }

    private CreateOrderApiResponse toCreateResponse(Order order) {
        return new CreateOrderApiResponse(
                order.getId(),
                order.getOrderUid(),
                order.getExternalRef(),
                order.getStatus(),
                order.getLastErrorMessage(),
                order.getCreatedAt()
        );
    }

    private OrderSummaryResponse toSummaryResponse(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderUid(),
                order.getExternalRef(),
                order.getStatus(),
                order.getLastErrorMessage(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderDetailResponse toDetailResponse(Order order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderUid(),
                order.getExternalRef(),
                order.getStatus(),
                order.getLastErrorMessage(),
                fromJson(order.getRequestPayload()),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private record OrderPreparation(Order existingCreatedOrder) {
    }
}
