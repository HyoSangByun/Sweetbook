package com.sweetbook.server.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sweetbook.server.activity.domain.Activity;
import com.sweetbook.server.activity.repository.ActivityRepository;
import com.sweetbook.server.album.domain.AlbumActivity;
import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.domain.AlbumProjectStatus;
import com.sweetbook.server.album.domain.BookGenerationStatus;
import com.sweetbook.server.album.repository.AlbumActivityRepository;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.common.exception.BusinessException;
import com.sweetbook.server.common.exception.ErrorCode;
import com.sweetbook.server.photo.domain.ActivityPhoto;
import com.sweetbook.server.photo.dto.ActivityPhotoUploadResponse;
import com.sweetbook.server.photo.repository.ActivityPhotoRepository;
import com.sweetbook.server.user.domain.User;
import com.sweetbook.server.user.domain.UserRole;
import com.sweetbook.server.user.repository.UserRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(properties = {
        "app.photo.storage-dir=build/test-uploads",
        "app.photo.max-file-size-bytes=10485760"
})
class ActivityPhotoServiceTest {

    @Autowired
    private ActivityPhotoService activityPhotoService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private AlbumProjectRepository albumProjectRepository;

    @Autowired
    private AlbumActivityRepository albumActivityRepository;

    @Autowired
    private ActivityPhotoRepository activityPhotoRepository;

    @AfterEach
    void tearDown() throws IOException {
        activityPhotoRepository.deleteAll();
        albumActivityRepository.deleteAll();
        albumProjectRepository.deleteAll();
        activityRepository.deleteAll();
        userRepository.deleteAll();
        Files.createDirectories(Path.of("build"));
        if (Files.exists(Path.of("build/test-uploads"))) {
            Files.walk(Path.of("build/test-uploads"))
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    @Test
    void 다른_사용자_앨범에는_사진을_업로드할_수_없다() {
        User owner = userRepository.save(newUser("photo-owner@sweetbook.com"));
        User attacker = userRepository.save(newUser("photo-attacker@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newAlbum(owner));
        Activity activity = activityRepository.save(newActivity(owner, "P-1001"));
        albumActivityRepository.save(AlbumActivity.builder()
                .albumProject(album)
                .activity(activity)
                .build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "a.png",
                "image/png",
                "abc".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> activityPhotoService.uploadPhoto(attacker.getId(), album.getId(), activity.getId(), file))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ALBUM_NOT_FOUND));
    }

    @Test
    void 업로드와_삭제는_afterCommit_파일처리로_반영된다() {
        User user = userRepository.save(newUser("photo-file@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newAlbum(user));
        Activity activity = activityRepository.save(newActivity(user, "P-2001"));
        albumActivityRepository.save(AlbumActivity.builder()
                .albumProject(album)
                .activity(activity)
                .build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                "png-bytes".getBytes(StandardCharsets.UTF_8)
        );

        ActivityPhotoUploadResponse upload = activityPhotoService.uploadPhoto(user.getId(), album.getId(), activity.getId(), file);
        ActivityPhoto saved = activityPhotoRepository.findById(upload.photoId()).orElseThrow();
        Path storedPath = Path.of(saved.getStoragePath());

        assertThat(Files.exists(storedPath)).isTrue();

        activityPhotoService.deletePhoto(user.getId(), album.getId(), activity.getId(), upload.photoId());

        assertThat(activityPhotoRepository.findById(upload.photoId())).isEmpty();
        assertThat(Files.exists(storedPath)).isFalse();
    }

    private User newUser(String email) {
        return User.builder()
                .email(email)
                .password("encoded-password")
                .role(UserRole.USER)
                .build();
    }

    private AlbumProject newAlbum(User user) {
        return AlbumProject.builder()
                .user(user)
                .month("2026-04")
                .title("photo-album")
                .subtitle("sub")
                .monthlyReview("review")
                .status(AlbumProjectStatus.DRAFT)
                .bookStatus(BookGenerationStatus.NOT_GENERATED)
                .build();
    }

    private Activity newActivity(User user, String externalId) {
        return Activity.builder()
                .user(user)
                .externalActivityId(externalId)
                .activityType("Run")
                .activityName("Photo Run")
                .activityDateTime(LocalDateTime.of(2026, 4, 1, 7, 0))
                .activityMonth("2026-04")
                .distanceKm(3.0)
                .movingTimeSeconds(1000)
                .elapsedTimeSeconds(1100)
                .averageSpeed(2.8)
                .elevationGain(25.0)
                .calories(180)
                .build();
    }
}

