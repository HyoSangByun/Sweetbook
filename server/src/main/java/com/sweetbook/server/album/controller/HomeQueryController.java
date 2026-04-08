package com.sweetbook.server.album.controller;

import com.sweetbook.server.common.response.ApiResponse;
import com.sweetbook.server.order.dto.CancelOrderApiRequest;
import com.sweetbook.server.order.dto.UpdateOrderShippingByUidRequest;
import com.sweetbook.server.order.service.OrderService;
import com.sweetbook.server.sweetbook.service.AlbumBookGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Home Query", description = "홈 화면 전역 조회 API")
public class HomeQueryController {

    private final OrderService orderService;
    private final AlbumBookGenerationService albumBookGenerationService;

    @GetMapping("/orders")
    @Operation(summary = "전체 주문 조회", description = "현재 사용자의 모든 주문 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAllOrders() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.listAllOrders()));
    }

    @PostMapping("/orders/{orderUid}/cancel")
    @Operation(summary = "주문 취소", description = "orderUid로 SweetBook 주문을 취소합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelOrderByUid(
            @PathVariable String orderUid,
            @Valid @RequestBody CancelOrderApiRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.cancelOrderByUid(orderUid, request)));
    }

    @PatchMapping("/orders/{orderUid}/shipping")
    @Operation(summary = "주문 배송지 수정", description = "orderUid로 수령인/기본주소를 수정합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateShippingByUid(
            @PathVariable String orderUid,
            @Valid @RequestBody UpdateOrderShippingByUidRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.updateOrderShippingByUid(orderUid, request)));
    }

    @GetMapping("/books")
    @Operation(summary = "전체 책 조회", description = "현재 사용자가 생성한 모든 책 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listAllBooks() {
        return ResponseEntity.ok(ApiResponse.ok(albumBookGenerationService.listAllUserBooks()));
    }
}
