package com.sweetbook.server.activity.controller;

import com.sweetbook.server.activity.dto.ActivityDetailResponse;
import com.sweetbook.server.activity.dto.ActivityImportResponse;
import com.sweetbook.server.activity.dto.ActivityMonthResponse;
import com.sweetbook.server.activity.dto.ActivityStatsResponse;
import com.sweetbook.server.activity.dto.ActivitySummaryResponse;
import com.sweetbook.server.activity.service.ActivityCsvImportService;
import com.sweetbook.server.activity.service.ActivityService;
import com.sweetbook.server.common.response.ApiResponse;
import com.sweetbook.server.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities")
@Tag(name = "Activity", description = "운동 데이터 조회/통계/CSV 적재 API")
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityCsvImportService activityCsvImportService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "CSV 운동 데이터 적재", description = "사용자 CSV 파일을 파싱해 운동 기록을 적재합니다.")
    public ResponseEntity<ApiResponse<ActivityImportResponse>> importCsv(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityCsvImportService.importCsv(principal.getUserId(), file)));
    }

    @GetMapping("/months")
    @Operation(summary = "운동 월 목록 조회", description = "운동 기록이 있는 월 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ActivityMonthResponse>>> getMonths(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.getMonths(principal.getUserId())));
    }

    @GetMapping
    @Operation(summary = "월별 운동 목록 조회", description = "지정 월(YYYY-MM)의 운동 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ActivitySummaryResponse>>> getActivitiesByMonth(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("month") String month
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.getActivitiesByMonth(principal.getUserId(), month)));
    }

    @GetMapping("/{activityId}")
    @Operation(summary = "운동 상세 조회", description = "운동 기록 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<ActivityDetailResponse>> getActivityDetail(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable("activityId") Long activityId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.getActivityDetail(principal.getUserId(), activityId)));
    }

    @GetMapping("/stats")
    @Operation(summary = "월별 통계 조회", description = "지정 월의 운동 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<ActivityStatsResponse>> getMonthlyStats(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("month") String month
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.getMonthlyStats(principal.getUserId(), month)));
    }
}

