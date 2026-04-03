package com.sweetbook.server.activity.controller;

import com.sweetbook.server.activity.dto.ActivityDetailResponse;
import com.sweetbook.server.activity.dto.ActivityMonthResponse;
import com.sweetbook.server.activity.dto.ActivityStatsResponse;
import com.sweetbook.server.activity.dto.ActivitySummaryResponse;
import com.sweetbook.server.activity.service.ActivityService;
import com.sweetbook.server.common.response.ApiResponse;
import com.sweetbook.server.security.AppUserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/months")
    public ResponseEntity<ApiResponse<List<ActivityMonthResponse>>> getMonths(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.getMonths(principal.getUserId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivitySummaryResponse>>> getActivitiesByMonth(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("month") String month
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.getActivitiesByMonth(principal.getUserId(), month)));
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<ApiResponse<ActivityDetailResponse>> getActivityDetail(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable("activityId") Long activityId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.getActivityDetail(principal.getUserId(), activityId)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ActivityStatsResponse>> getMonthlyStats(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("month") String month
    ) {
        return ResponseEntity.ok(ApiResponse.ok(activityService.getMonthlyStats(principal.getUserId(), month)));
    }
}

