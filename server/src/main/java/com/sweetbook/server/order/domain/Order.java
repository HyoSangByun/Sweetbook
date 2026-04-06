package com.sweetbook.server.order.domain;

import com.sweetbook.server.album.domain.AlbumProject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Builder
@Entity
@Table(
        name = "album_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_album_orders_album_external_ref", columnNames = {"album_project_id", "external_ref"}),
                @UniqueConstraint(name = "uk_album_orders_order_uid", columnNames = {"order_uid"})
        },
        indexes = {
                @Index(name = "idx_album_orders_album_project", columnList = "album_project_id"),
                @Index(name = "idx_album_project_created_at", columnList = "album_project_id, created_at DESC")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_project_id", nullable = false)
    private AlbumProject albumProject;

    @Column(name = "order_uid", length = 120)
    private String orderUid;

    @Column(name = "external_ref", nullable = false, length = 120)
    private String externalRef;

    @Column(name = "request_payload", nullable = false, length = 8000)
    private String requestPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Column(name = "remote_order_status_code")
    private Integer remoteOrderStatusCode;

    @Column(name = "remote_order_status_display", length = 120)
    private String remoteOrderStatusDisplay;

    @Column(name = "remote_ordered_at")
    private LocalDateTime remoteOrderedAt;

    @Column(name = "last_webhook_delivery_id", length = 120)
    private String lastWebhookDeliveryId;

    @Column(name = "last_webhook_event_type", length = 120)
    private String lastWebhookEventType;

    @Column(name = "last_webhook_received_at")
    private LocalDateTime lastWebhookReceivedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void markRequested(String requestPayload) {
        this.requestPayload = requestPayload;
        this.status = OrderStatus.REQUESTED;
        this.lastErrorMessage = null;
    }

    public void updateRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public void markCreated(String orderUid) {
        this.orderUid = orderUid;
        this.status = OrderStatus.CREATED;
        this.lastErrorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = OrderStatus.FAILED;
        this.lastErrorMessage = errorMessage;
    }

    public void markCompleted() {
        this.status = OrderStatus.COMPLETED;
        this.lastErrorMessage = null;
    }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
        this.lastErrorMessage = null;
    }

    public void updateRemoteStatus(Integer remoteOrderStatusCode, String remoteOrderStatusDisplay, LocalDateTime remoteOrderedAt) {
        this.remoteOrderStatusCode = remoteOrderStatusCode;
        this.remoteOrderStatusDisplay = remoteOrderStatusDisplay;
        this.remoteOrderedAt = remoteOrderedAt;
    }

    public void updateWebhookMetadata(String deliveryId, String eventType, LocalDateTime receivedAt) {
        this.lastWebhookDeliveryId = deliveryId;
        this.lastWebhookEventType = eventType;
        this.lastWebhookReceivedAt = receivedAt;
    }
}
