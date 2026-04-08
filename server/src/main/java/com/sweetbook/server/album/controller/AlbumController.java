package com.sweetbook.server.album.controller;

import com.sweetbook.server.album.dto.AlbumResponse;
import com.sweetbook.server.album.dto.AddBookContentsRequest;
import com.sweetbook.server.album.dto.ApplyBookCoverRequest;
import com.sweetbook.server.album.dto.CreateAlbumRequest;
import com.sweetbook.server.album.dto.CreateBookDraftRequest;
import com.sweetbook.server.album.dto.CreateBookDraftResponse;
import com.sweetbook.server.album.dto.DeselectAlbumActivityResponse;
import com.sweetbook.server.album.dto.FinalizeBookResponse;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesRequest;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesResponse;
import com.sweetbook.server.album.dto.UploadBookPhotoResponse;
import com.sweetbook.server.album.dto.UpdateAlbumRequest;
import com.sweetbook.server.album.service.AlbumService;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
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
import java.util.Locale;
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

    @GetMapping("/book-specs/{bookSpecUid}")
    @Operation(summary = "Sweetbook 판형 상세 조회", description = "선택한 판형의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookSpecDetail(@PathVariable String bookSpecUid) {
        String normalizedBookSpecUid = bookSpecUid == null ? "" : bookSpecUid.trim();
        if (normalizedBookSpecUid.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "bookSpecUid must be provided and non-blank"
            );
        }
        return ResponseEntity.ok(ApiResponse.ok(sweetbookCatalogService.getBookSpecDetail(normalizedBookSpecUid)));
    }

    @GetMapping("/templates")
    @Operation(summary = "Sweetbook 템플릿 목록 조회", description = "선택한 판형과 템플릿 종류로 템플릿 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTemplates(
            @RequestParam String bookSpecUid,
            @RequestParam String templateKind
    ) {
        String normalizedBookSpecUid = bookSpecUid == null ? "" : bookSpecUid.trim();
        if (normalizedBookSpecUid.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "bookSpecUid must be provided and non-blank"
            );
        }

        String normalizedTemplateKind = templateKind == null ? "" : templateKind.trim().toLowerCase(Locale.ROOT);
        if (!"cover".equals(normalizedTemplateKind) && !"content".equals(normalizedTemplateKind)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "templateKind must be one of: cover, content"
            );
        }

        return ResponseEntity.ok(ApiResponse.ok(
                sweetbookCatalogService.getTemplates(normalizedBookSpecUid, normalizedTemplateKind)
        ));
    }

    @GetMapping("/templates/{templateUid}")
    @Operation(summary = "Sweetbook 템플릿 상세 조회", description = "템플릿 썸네일 등 상세 메타데이터를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTemplateDetail(@PathVariable String templateUid) {
        return ResponseEntity.ok(ApiResponse.ok(sweetbookCatalogService.getTemplateDetail(templateUid)));
    }

    @PostMapping("/{albumId}/book/draft")
    @Operation(summary = "책 Draft 생성", description = "활동 선택 완료 후 SweetBook 책 draft를 생성합니다.")
    public ResponseEntity<ApiResponse<CreateBookDraftResponse>> createDraftBook(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody CreateBookDraftRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                albumBookGenerationService.createDraftBook(principal.getUserId(), albumId, request)
        ));
    }

    @PostMapping(value = "/{albumId}/book/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "책 사진 업로드", description = "생성된 bookUid 기준으로 SweetBook 사진 업로드를 수행합니다.")
    public ResponseEntity<ApiResponse<UploadBookPhotoResponse>> uploadBookPhoto(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                albumBookGenerationService.uploadBookPhoto(principal.getUserId(), albumId, file)
        ));
    }

    @GetMapping("/{albumId}/book/photos")
    @Operation(summary = "책 사진 목록 조회", description = "GET /v1/books/{bookUid}/photos 결과를 반환합니다.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listBookPhotos(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                albumBookGenerationService.listBookPhotos(principal.getUserId(), albumId)
        ));
    }

    @PostMapping("/{albumId}/book/cover")
    @Operation(summary = "표지 추가", description = "고정 커버 템플릿(tpl_F8d15af9fd)으로 표지를 적용합니다.")
    public ResponseEntity<ApiResponse<Void>> applyBookCover(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody ApplyBookCoverRequest request
    ) {
        albumBookGenerationService.applyCover(principal.getUserId(), albumId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{albumId}/book/contents")
    @Operation(summary = "내지 추가", description = "고정 내지 템플릿(3T09l6GEd0AL)으로 활동별 내지를 추가합니다.")
    public ResponseEntity<ApiResponse<Void>> addBookContents(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody AddBookContentsRequest request
    ) {
        albumBookGenerationService.addContents(principal.getUserId(), albumId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{albumId}/book/finalization")
    @Operation(summary = "책 최종화", description = "커버/내지 적용 완료 후 책을 최종화합니다.")
    public ResponseEntity<ApiResponse<FinalizeBookResponse>> finalizeBook(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                albumBookGenerationService.finalizeBook(principal.getUserId(), albumId)
        ));
    }

    @GetMapping("/{albumId}/books")
    @Operation(summary = "앨범 생성 책 목록", description = "GET /v1/books를 기반으로 현재 앨범에서 생성한 책 목록을 반환합니다.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAlbumBooks(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                albumBookGenerationService.listAlbumBooks(principal.getUserId(), albumId)
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
