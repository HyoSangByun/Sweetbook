package com.sweetbook.server.album.controller;

import com.sweetbook.server.album.dto.AlbumResponse;
import com.sweetbook.server.album.dto.CreateAlbumRequest;
import com.sweetbook.server.album.dto.DeselectAlbumActivityResponse;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesRequest;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesResponse;
import com.sweetbook.server.album.dto.UpdateAlbumRequest;
import com.sweetbook.server.album.service.AlbumService;
import com.sweetbook.server.common.response.ApiResponse;
import com.sweetbook.server.photo.dto.ActivityPhotoDeleteResponse;
import com.sweetbook.server.photo.dto.ActivityPhotoItemResponse;
import com.sweetbook.server.photo.dto.ActivityPhotoUploadResponse;
import com.sweetbook.server.photo.service.ActivityPhotoService;
import com.sweetbook.server.security.AppUserPrincipal;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/albums")
public class AlbumController {

    private final AlbumService albumService;
    private final ActivityPhotoService activityPhotoService;

    @PostMapping
    public ResponseEntity<ApiResponse<AlbumResponse>> createAlbum(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateAlbumRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(albumService.createAlbum(principal.getUserId(), request)));
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<ApiResponse<AlbumResponse>> getAlbum(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(albumService.getAlbum(principal.getUserId(), albumId)));
    }

    @PatchMapping("/{albumId}")
    public ResponseEntity<ApiResponse<AlbumResponse>> updateAlbum(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @Valid @RequestBody UpdateAlbumRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(albumService.updateAlbum(principal.getUserId(), albumId, request)));
    }

    @PostMapping("/{albumId}/activities")
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
    public ResponseEntity<ApiResponse<List<ActivityPhotoItemResponse>>> listActivityPhotos(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long albumId,
            @PathVariable Long activityId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                activityPhotoService.listPhotos(principal.getUserId(), albumId, activityId)
        ));
    }
}
