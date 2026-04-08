package com.sweetbook.server.credit.controller;

import com.sweetbook.server.common.response.ApiResponse;
import com.sweetbook.server.credit.dto.ChargeCreditRequest;
import com.sweetbook.server.credit.dto.ChargeCreditResponse;
import com.sweetbook.server.credit.dto.CreditBalanceResponse;
import com.sweetbook.server.credit.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/credits")
@Tag(name = "Credits", description = "크레딧 조회 API")
public class CreditController {

    private final CreditService creditService;

    @GetMapping
    @Operation(summary = "크레딧 잔액 조회", description = "연동된 Sweetbook 계정의 크레딧 잔액을 조회합니다.")
    public ResponseEntity<ApiResponse<CreditBalanceResponse>> getCreditsBalance() {
        return ResponseEntity.ok(ApiResponse.ok(creditService.getCreditsBalance()));
    }

    @PostMapping("/sandbox/charge")
    @Operation(summary = "샌드박스 크레딧 충전", description = "amount를 받아 샌드박스 크레딧을 충전합니다.")
    public ResponseEntity<ApiResponse<ChargeCreditResponse>> chargeSandboxCredits(
            @Valid @RequestBody ChargeCreditRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(creditService.chargeSandboxCredits(request.amount())));
    }
}
