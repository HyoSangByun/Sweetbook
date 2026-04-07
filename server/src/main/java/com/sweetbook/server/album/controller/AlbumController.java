package com.sweetbook.server.album.controller;

import com.sweetbook.server.album.dto.AlbumResponse;
import com.sweetbook.server.album.dto.BookEstimateRequest;
import com.sweetbook.server.album.dto.BookEstimateResponse;
import com.sweetbook.server.album.dto.CreateAlbumRequest;
import com.sweetbook.server.album.dto.DeselectAlbumActivityResponse;
import com.sweetbook.server.album.dto.GenerateBookResponse;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesRequest;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesResponse;
import com.sweetbook.server.album.dto.UpdateAlbumRequest;
import com.sweetbook.server.album.service.AlbumService;
import com.sweetbook.server.common.response.ApiResponse;
import com.sweetbook.server.order.dto.CreateOrderApiRequest;
import com.sweetbook.server.order.dto.CreateOrderApiResponse;
import com.sweetbook.server.order.dto.CancelOrderApiRequest;
import com.sweetbook.server.order.dto.OrderDetailResponse;
import com.sweetbook.server.order.dto.OrderSummaryResponse;
import com.sweetbook.server.order.dto.UpdateOrderShippingRequest;
import com.sweetbook.server.order.service.OrderService;
import com.sweetbook.server.photo.dto.ActivityPhotoDeleteResponse;
import com.sweetbook.server.photo.dto.ActivityPhotoItemResponse;
import com.sweetbook.server.photo.dto.ActivityPhotoUploadResponse;
import com.sweetbook.server.photo.service.ActivityPhotoService;
import com.sweetbook.server.security.AppUserPrincipal;
import com.sweetbook.server.sweetbook.service.AlbumBookGenerationService;
import com.sweetbook.server.sweetbook.service.SweetbookCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/albums")
@Tag(name = "Album", description = "앨범, 사진, 책 생성, 주문 API")
public class AlbumController {

    private final AlbumService albumService;
    private final ActivityPhotoService activityPhotoService;
    private final AlbumBookGenerationService albumBookGenerationService;
    private final SweetbookCatalogService sweetbookCatalogService;
    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "앨범 생성", description = "선택한 월 기준으로 앨범 초안을 생성합니다.")
    public ResponseEntity<ApiResponse<AlbumResponse>> createAlbum(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateAlbumRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(albumService.createAlbum(principal.getUserId(), request)));
    }

    @GetMapping("/{albumId}")
    @Operation(summary = "앨범 상세 조회", description = "앨범 정보와 선택된 운동 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<AlbumResponse>> getAlbum(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(albumService.getAlbum(principal.getUserId(), albumId)));
    }

    @PatchMapping("/{albumId}")
    @Operation(summary = "앨범 수정", description = "제목, 부제, 월간 회고를 수정합니다.")
    public ResponseEntity<ApiResponse<AlbumResponse>> updateAlbum(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody UpdateAlbumRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(albumService.updateAlbum(principal.getUserId(), albumId, request)));
    }

    @PostMapping("/{albumId}/activities")
    @Operation(summary = "앨범 운동 선택", description = "앨범에 포함할 운동 기록을 선택합니다.")
    public ResponseEntity<ApiResponse<SelectAlbumActivitiesResponse>> selectAlbumActivities(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody SelectAlbumActivitiesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                albumService.selectAlbumActivities(principal.getUserId(), albumId, request)
        ));
    }

    @DeleteMapping("/{albumId}/activities/{activityId}")
    @Operation(summary = "앨범 운동 선택 해제", description = "앨범에서 특정 운동 기록 선택을 해제합니다.")
    public ResponseEntity<ApiResponse<DeselectAlbumActivityResponse>> deselectAlbumActivity(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                albumService.deselectAlbumActivity(principal.getUserId(), albumId, activityId)
        ));
    }

    @PostMapping(value = "/{albumId}/activities/{activityId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "운동 사진 업로드", description = "선택된 운동 기록에 사진을 업로드합니다.")
    public ResponseEntity<ApiResponse<ActivityPhotoUploadResponse>> uploadActivityPhoto(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @PathVariable Long activityId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                activityPhotoService.uploadPhoto(principal.getUserId(), albumId, activityId, file)
        ));
    }

    @DeleteMapping("/{albumId}/activities/{activityId}/photos/{photoId}")
    @Operation(summary = "운동 사진 삭제", description = "선택된 운동 기록의 사진을 삭제합니다.")
    public ResponseEntity<ApiResponse<ActivityPhotoDeleteResponse>> deleteActivityPhoto(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @PathVariable Long activityId,
            @PathVariable Long photoId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                activityPhotoService.deletePhoto(principal.getUserId(), albumId, activityId, photoId)
        ));
    }

    @GetMapping("/{albumId}/activities/{activityId}/photos")
    @Operation(summary = "운동 사진 목록 조회", description = "선택된 운동 기록의 사진 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ActivityPhotoItemResponse>>> listActivityPhotos(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                activityPhotoService.listPhotos(principal.getUserId(), albumId, activityId)
        ));
    }

    @GetMapping("/book-specs")
    @Operation(summary = "Sweetbook 판형 목록 조회", description = "도서 생성 폼에 필요한 판형 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBookSpecs() {
        return ResponseEntity.ok(ApiResponse.ok(sweetbookCatalogService.getBookSpecs()));
    }

    @GetMapping("/templates")
    @Operation(summary = "Sweetbook 템플릿 목록 조회", description = "선택한 판형과 템플릿 종류로 템플릿 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTemplates(
            @RequestParam String bookSpecUid,
            @RequestParam String templateKind
    ) {
        return ResponseEntity.ok(ApiResponse.ok(sweetbookCatalogService.getTemplates(bookSpecUid, templateKind)));
    }

    @GetMapping("/templates/{templateUid}")
    @Operation(summary = "Sweetbook 템플릿 상세 조회", description = "템플릿 썸네일 등 상세 메타데이터를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTemplateDetail(@PathVariable String templateUid) {
        return ResponseEntity.ok(ApiResponse.ok(sweetbookCatalogService.getTemplateDetail(templateUid)));
    }

    @PostMapping("/{albumId}/book")
    @Operation(summary = "책 생성", description = "앨범 기준으로 Sweetbook 책을 생성하고 최종 확정합니다.")
    public ResponseEntity<ApiResponse<GenerateBookResponse>> generateBook(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                albumBookGenerationService.generateBook(principal.getUserId(), albumId)
        ));
    }

    @PostMapping("/{albumId}/book/estimate")
    @Operation(summary = "책 미리보기 견적", description = "선택한 입력값으로 예상 페이지수와 주문 금액을 계산합니다.")
    public ResponseEntity<ApiResponse<BookEstimateResponse>> estimateBookOrder(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody BookEstimateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                albumBookGenerationService.estimateOrder(principal.getUserId(), albumId, request)
        ));
    }

    @PostMapping("/{albumId}/orders")
    @Operation(summary = "주문 생성", description = "생성된 책을 기준으로 주문을 생성합니다.")
    public ResponseEntity<ApiResponse<CreateOrderApiResponse>> createOrder(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody CreateOrderApiRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.createOrder(principal.getUserId(), albumId, request)
        ));
    }

    @GetMapping("/{albumId}/orders")
    @Operation(summary = "주문 목록 조회", description = "앨범에 연결된 주문 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> listOrders(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.listOrders(principal.getUserId(), albumId)
        ));
    }

    @GetMapping("/{albumId}/orders/{orderId}")
    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.getOrder(principal.getUserId(), albumId, orderId)
        ));
    }

    @PostMapping("/{albumId}/orders/{orderId}/cancel")
    @Operation(summary = "주문 취소", description = "주문을 취소합니다.")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> cancelOrder(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @PathVariable Long orderId,
            @Valid @RequestBody CancelOrderApiRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.cancelOrder(principal.getUserId(), albumId, orderId, request)
        ));
    }

    @PatchMapping("/{albumId}/orders/{orderId}/shipping")
    @Operation(summary = "주문 배송지 변경", description = "주문 배송지 정보를 변경합니다.")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateOrderShipping(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderShippingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                orderService.updateOrderShipping(principal.getUserId(), albumId, orderId, request)
        ));
    }
}
