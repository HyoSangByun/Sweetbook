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
import com.sweetbook.server.order.dto.CancelOrderApiRequest;
import com.sweetbook.server.order.dto.CreateOrderApiRequest;
import com.sweetbook.server.order.dto.CreateOrderApiResponse;
import com.sweetbook.server.order.dto.OrderDetailResponse;
import com.sweetbook.server.order.dto.OrderSummaryResponse;
import com.sweetbook.server.order.dto.UpdateOrderShippingRequest;
import com.sweetbook.server.order.repository.OrderRepository;
import com.sweetbook.server.sweetbook.client.SweetbookOrdersClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int ORDER_REQUEST_PAYLOAD_MAX_LENGTH = 8000;
    private static final ObjectMapper CANONICAL_OBJECT_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private final AlbumProjectRepository albumProjectRepository;
    private final OrderRepository orderRepository;
    private final SweetbookOrdersClient sweetbookOrdersClient;
    private final PlatformTransactionManager transactionManager;

    public CreateOrderApiResponse createOrder(Long userId, Long albumId, CreateOrderApiRequest request) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        validateOrderableAlbum(albumProject);

        Map<String, Object> payload = buildBasePayload(request, albumProject.getBookUid());
        String payloadJsonForRef = toCanonicalJson(payload);
        String externalRef = resolveExternalRef(albumId, request.externalRef(), payloadJsonForRef);
        payload.put("externalRef", externalRef);
        String payloadJson = toCanonicalJson(payload);
        validateRequestPayloadLength(payloadJson, albumId, externalRef);
        String idempotencyKey = "order-" + albumId + "-" + externalRef;

        OrderPreparation preparation = prepareOrder(albumProject, externalRef, payloadJson);
        if (preparation.existingOrder() != null) {
            return toCreateResponse(preparation.existingOrder());
        }

        final String orderUid;
        try {
            orderUid = sweetbookOrdersClient.createOrder(payload, idempotencyKey);
        } catch (BusinessException ex) {
            markFailed(userId, albumId, externalRef, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markFailed(userId, albumId, externalRef, ex.getMessage());
            throw ex;
        }

        return markCreated(userId, albumId, externalRef, orderUid);
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
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "二쇰Ц??李얠쓣 ???놁뒿?덈떎."));
        return toDetailResponse(order);
    }
    public OrderDetailResponse cancelOrder(Long userId, Long albumId, Long orderId, CancelOrderApiRequest request) {
        Order order = getOwnedOrder(userId, albumId, orderId);
        validateCancelable(order);
        if (order.getOrderUid() == null || order.getOrderUid().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "orderUid媛 ?놁뼱 二쇰Ц??痍⑥냼?????놁뒿?덈떎.");
        }

        String orderUid = order.getOrderUid();
        Map<String, Object> response = sweetbookOrdersClient.cancelOrder(orderUid, request.cancelReason());

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> {
            Order managedOrder = getOwnedOrder(userId, albumId, orderId);
            syncRemoteFromResponse(managedOrder, response, 80, "CANCELLED");
            managedOrder.markCancelled();
            orderRepository.save(managedOrder);
            return toDetailResponse(managedOrder);
        });
    }
    public OrderDetailResponse updateOrderShipping(Long userId, Long albumId, Long orderId, UpdateOrderShippingRequest request) {
        Map<String, Object> patch = request.toPatchMap();
        if (patch.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "諛곗넚吏 蹂寃??꾨뱶瑜?1媛??댁긽 ?낅젰?댁빞 ?⑸땲??");
        }

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> {
            Order order = getOwnedOrder(userId, albumId, orderId);
            validateShippingUpdatable(order);
            if (order.getOrderUid() == null || order.getOrderUid().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "orderUid媛 ?놁뼱 諛곗넚吏瑜?蹂寃쏀븷 ???놁뒿?덈떎.");
            }

            Map<String, Object> response = sweetbookOrdersClient.updateShipping(order.getOrderUid(), patch);
            syncRemoteFromResponse(order, response, order.getRemoteOrderStatusCode(), order.getRemoteOrderStatusDisplay());
            mergeShippingToStoredPayload(order, patch);
            return toDetailResponse(order);
        });
    }

    public void applyWebhookStatusUpdate(
            String orderUid,
            Integer remoteOrderStatusCode,
            String remoteOrderStatusDisplay,
            LocalDateTime remoteOrderedAt,
            String deliveryId,
            String eventType
    ) {
        if (orderUid == null || orderUid.isBlank()) {
            return;
        }

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> orderRepository.findByOrderUid(orderUid).ifPresent(order -> {
            if (deliveryId != null && deliveryId.equals(order.getLastWebhookDeliveryId())) {
                return;
            }

            Integer currentCode = order.getRemoteOrderStatusCode();
            if (isOutdatedStatus(currentCode, remoteOrderStatusCode, eventType)) {
                order.updateWebhookMetadata(deliveryId, eventType, LocalDateTime.now());
                return;
            }

            if (remoteOrderStatusCode != null) {
                order.updateRemoteStatus(remoteOrderStatusCode, remoteOrderStatusDisplay, remoteOrderedAt);
                OrderStatus mapped = mapRemoteToLocalStatus(remoteOrderStatusCode);
                if (mapped != null && canTransition(order.getStatus(), mapped, eventType)) {
                    applyMappedStatus(order, mapped, remoteOrderStatusCode, remoteOrderStatusDisplay, eventType);
                }
            }

            order.updateWebhookMetadata(deliveryId, eventType, LocalDateTime.now());
        }));
    }

    public void applyWebhookStatusUpdateByEvent(
            String orderUid,
            String webhookEvent,
            String status,
            LocalDateTime eventAt,
            String deliveryId
    ) {
        WebhookMappedStatus mappedStatus = mapWebhookEventStatus(webhookEvent, status);
        if (mappedStatus == null) {
            return;
        }
        applyWebhookStatusUpdate(
                orderUid,
                mappedStatus.code(),
                mappedStatus.display(),
                eventAt,
                deliveryId,
                webhookEvent
        );
    }

    private OrderPreparation prepareOrder(AlbumProject albumProject, String externalRef, String payloadJson) {
        Long albumId = albumProject.getId();
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> {
            Order existing = orderRepository.findByAlbumProjectIdAndExternalRef(albumId, externalRef).orElse(null);
            if (existing != null) {
                if (existing.getStatus() != OrderStatus.FAILED) {
                    return new OrderPreparation(existing);
                }
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
                if (conflicted.getStatus() != OrderStatus.FAILED) {
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
                            "二쇰Ц ?곹깭 ?덉퐫?쒕? 李얠쓣 ???놁뒿?덈떎."
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
                    .ifPresent(order -> {
                        if (order.getStatus() == OrderStatus.REQUESTED || order.getStatus() == OrderStatus.FAILED) {
                            order.markFailed(trimError(errorMessage));
                        }
                    });
        });
    }

    private AlbumProject getOwnedAlbum(Long userId, Long albumId) {
        return albumProjectRepository.findByIdAndUserId(albumId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALBUM_NOT_FOUND));
    }

    private Order getOwnedOrder(Long userId, Long albumId, Long orderId) {
        AlbumProject albumProject = getOwnedAlbum(userId, albumId);
        return orderRepository.findByIdAndAlbumProjectId(orderId, albumProject.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "二쇰Ц??李얠쓣 ???놁뒿?덈떎."));
    }

    private void validateCancelable(Order order) {
        Integer remoteCode = order.getRemoteOrderStatusCode();
        if (remoteCode == null) {
            if (order.getStatus() == OrderStatus.CREATED) {
                return;
            }
            throw new BusinessException(ErrorCode.INVALID_INPUT, "痍⑥냼 媛?ν븳 二쇰Ц ?곹깭媛 ?꾨떃?덈떎.");
        }
        if (remoteCode != 20 && remoteCode != 25) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "PAID ?먮뒗 PDF_READY ?곹깭?먯꽌留?痍⑥냼?????덉뒿?덈떎.");
        }
    }

    private void validateShippingUpdatable(Order order) {
        Integer remoteCode = order.getRemoteOrderStatusCode();
        if (remoteCode == null) {
            if (order.getStatus() == OrderStatus.CREATED) {
                return;
            }
            throw new BusinessException(ErrorCode.INVALID_INPUT, "諛곗넚吏 蹂寃?媛?ν븳 二쇰Ц ?곹깭媛 ?꾨떃?덈떎.");
        }
        if (remoteCode != 20 && remoteCode != 25 && remoteCode != 30) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "PAID, PDF_READY, CONFIRMED ?곹깭?먯꽌留?諛곗넚吏瑜?蹂寃쏀븷 ???덉뒿?덈떎.");
        }
    }

    private void validateOrderableAlbum(AlbumProject albumProject) {
        if (albumProject.getBookStatus() != BookGenerationStatus.GENERATED
                || albumProject.getBookUid() == null
                || albumProject.getBookUid().isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "梨??앹꽦???꾨즺???⑤쾾留?二쇰Ц?????덉뒿?덈떎."
            );
        }
    }

    private Map<String, Object> buildBasePayload(CreateOrderApiRequest request, String ownedBookUid) {
        List<Map<String, Object>> itemMaps = request.items().stream()
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookUid", ownedBookUid);
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
            BusinessException be = new BusinessException(ErrorCode.INVALID_INPUT, "二쇰Ц payload 吏곷젹?붿뿉 ?ㅽ뙣?덉뒿?덈떎.");
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

    private void mergeShippingToStoredPayload(Order order, Map<String, Object> patch) {
        Map<String, Object> payload = new LinkedHashMap<>(fromJson(order.getRequestPayload()));
        Object shippingRaw = payload.get("shipping");
        Map<String, Object> shipping = new LinkedHashMap<>();
        if (shippingRaw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                shipping.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        shipping.putAll(patch);
        payload.put("shipping", shipping);
        String json = toCanonicalJson(payload);
        validateRequestPayloadLength(json, order.getAlbumProject().getId(), order.getExternalRef());
        order.updateRequestPayload(json);
    }

    private byte[] digestSha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            BusinessException be = new BusinessException(ErrorCode.INTERNAL_ERROR, "?댁떆 ?뚭퀬由ъ쬁???ъ슜?????놁뒿?덈떎.");
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

    private void syncRemoteFromResponse(Order order, Map<String, Object> response, Integer defaultCode, String defaultDisplay) {
        Integer code = getInt(response, "orderStatus");
        String display = getString(response, "orderStatusDisplay");
        LocalDateTime orderedAt = parseOrderedAt(getString(response, "orderedAt"));

        Integer resolvedCode = code != null ? code : defaultCode;
        String resolvedDisplay = display != null ? display : defaultDisplay;
        order.updateRemoteStatus(resolvedCode, resolvedDisplay, orderedAt);

        if (resolvedCode != null) {
            OrderStatus mapped = mapRemoteToLocalStatus(resolvedCode);
            if (mapped != null) {
                applyMappedStatus(order, mapped, resolvedCode, resolvedDisplay, "remote.sync");
            }
        }
    }

    private Integer getInt(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private void validateRequestPayloadLength(String payloadJson, Long albumId, String externalRef) {
        if (payloadJson.length() <= ORDER_REQUEST_PAYLOAD_MAX_LENGTH) {
            return;
        }
        log.warn(
                "Order request payload too large. albumId={}, externalRef={}, payloadLength={}, maxLength={}",
                albumId,
                externalRef,
                payloadJson.length(),
                ORDER_REQUEST_PAYLOAD_MAX_LENGTH
        );
        throw new BusinessException(
                ErrorCode.INVALID_INPUT,
                "二쇰Ц ?붿껌 payload 湲몄씠??8000?먮? 珥덇낵?????놁뒿?덈떎."
        );
    }

    private boolean isOutdatedStatus(Integer currentCode, Integer incomingCode, String eventType) {
        if (currentCode == null || incomingCode == null) {
            return false;
        }
        if ("order.restored".equals(eventType)) {
            return false;
        }
        return statusRank(incomingCode) < statusRank(currentCode);
    }

    private OrderStatus mapRemoteToLocalStatus(int remoteCode) {
        if (remoteCode == 70) {
            return OrderStatus.COMPLETED;
        }
        if (remoteCode == 80 || remoteCode == 81) {
            return OrderStatus.CANCELLED;
        }
        if (remoteCode == 90) {
            return OrderStatus.FAILED;
        }
        if (remoteCode >= 20 && remoteCode < 70) {
            return OrderStatus.CREATED;
        }
        return null;
    }

    private int statusRank(int remoteCode) {
        return switch (remoteCode) {
            case 20 -> 1;
            case 25 -> 2;
            case 30 -> 3;
            case 40 -> 4;
            case 45 -> 5;
            case 50 -> 6;
            case 60 -> 7;
            case 70 -> 8;
            case 80, 81 -> 9;
            case 90 -> 10;
            default -> -1;
        };
    }

    private boolean canTransition(OrderStatus current, OrderStatus target, String eventType) {
        if (current == target) {
            return true;
        }
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            if (current == OrderStatus.CANCELLED
                    && target == OrderStatus.CREATED
                    && "order.restored".equals(eventType)) {
                return true;
            }
            return false;
        }
        if (current == OrderStatus.CREATED && target == OrderStatus.REQUESTED) {
            return false;
        }
        return true;
    }

    private void applyMappedStatus(Order order, OrderStatus mapped, int remoteCode, String remoteDisplay, String eventType) {
        switch (mapped) {
            case CREATED -> order.markCreated(order.getOrderUid());
            case COMPLETED -> order.markCompleted();
            case CANCELLED -> order.markCancelled();
            case FAILED -> order.markFailed(trimError("?먭꺽 二쇰Ц ?ㅻ쪟 ?곹깭(" + remoteCode + ", " + remoteDisplay + ")"));
            case REQUESTED -> {
            }
        }
    }

    private CreateOrderApiResponse toCreateResponse(Order order) {
        return new CreateOrderApiResponse(
                order.getId(),
                order.getOrderUid(),
                order.getExternalRef(),
                order.getStatus(),
                order.getLastErrorMessage(),
                order.getRemoteOrderStatusCode(),
                order.getRemoteOrderStatusDisplay(),
                order.getRemoteOrderedAt(),
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
                order.getRemoteOrderStatusCode(),
                order.getRemoteOrderStatusDisplay(),
                order.getRemoteOrderedAt(),
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
                order.getRemoteOrderStatusCode(),
                order.getRemoteOrderStatusDisplay(),
                order.getRemoteOrderedAt(),
                fromJson(order.getRequestPayload()),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public LocalDateTime parseOrderedAt(String orderedAt) {
        if (orderedAt == null || orderedAt.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(orderedAt).toLocalDateTime();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private WebhookMappedStatus mapWebhookEventStatus(String event, String status) {
        if (event == null && status == null) {
            return null;
        }
        String normalizedEvent = event == null ? "" : event.trim().toLowerCase();
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();

        if ("order.created".equals(normalizedEvent)
                || "order.restored".equals(normalizedEvent)
                || "PAID".equals(normalizedStatus)) {
            return new WebhookMappedStatus(20, "PAID");
        }
        if ("PDF_READY".equals(normalizedStatus)) {
            return new WebhookMappedStatus(25, "PDF_READY");
        }
        if ("production.confirmed".equals(normalizedEvent) || "CONFIRMED".equals(normalizedStatus)) {
            return new WebhookMappedStatus(30, "CONFIRMED");
        }
        if ("production.started".equals(normalizedEvent) || "IN_PRODUCTION".equals(normalizedStatus)) {
            return new WebhookMappedStatus(40, "IN_PRODUCTION");
        }
        if ("production.completed".equals(normalizedEvent) || "PRODUCTION_COMPLETE".equals(normalizedStatus)) {
            return new WebhookMappedStatus(50, "PRODUCTION_COMPLETE");
        }
        if ("shipping.departed".equals(normalizedEvent) || "SHIPPED".equals(normalizedStatus)) {
            return new WebhookMappedStatus(60, "SHIPPED");
        }
        if ("shipping.delivered".equals(normalizedEvent) || "DELIVERED".equals(normalizedStatus)) {
            return new WebhookMappedStatus(70, "DELIVERED");
        }
        if ("order.cancelled".equals(normalizedEvent) || "CANCELLED".equals(normalizedStatus)) {
            return new WebhookMappedStatus(80, "CANCELLED");
        }
        if ("CANCELLED_REFUND".equals(normalizedStatus)) {
            return new WebhookMappedStatus(81, "CANCELLED_REFUND");
        }
        if ("ERROR".equals(normalizedStatus)) {
            return new WebhookMappedStatus(90, "ERROR");
        }
        return null;
    }

    private record OrderPreparation(Order existingOrder) {
    }

    private record WebhookMappedStatus(int code, String display) {
    }
}
