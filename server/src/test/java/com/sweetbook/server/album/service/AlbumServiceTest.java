package com.sweetbook.server.album.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sweetbook.server.activity.domain.Activity;
import com.sweetbook.server.activity.repository.ActivityRepository;
import com.sweetbook.server.album.domain.AlbumActivity;
import com.sweetbook.server.album.domain.AlbumProject;
import com.sweetbook.server.album.domain.AlbumProjectStatus;
import com.sweetbook.server.album.dto.DeselectAlbumActivityResponse;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesRequest;
import com.sweetbook.server.album.dto.SelectAlbumActivitiesResponse;
import com.sweetbook.server.album.repository.AlbumActivityRepository;
import com.sweetbook.server.album.repository.AlbumProjectRepository;
import com.sweetbook.server.photo.domain.ActivityPhoto;
import com.sweetbook.server.photo.repository.ActivityPhotoRepository;
import com.sweetbook.server.user.domain.User;
import com.sweetbook.server.user.domain.UserRole;
import com.sweetbook.server.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AlbumServiceTest {

    @Autowired
    private AlbumService albumService;

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
    void tearDown() {
        activityPhotoRepository.deleteAll();
        albumActivityRepository.deleteAll();
        albumProjectRepository.deleteAll();
        activityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 같은_운동을_재선택하면_skippedCount가_증가한다() {
        User user = userRepository.save(newUser("album-test-dup@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newAlbum(user, "2026-04"));
        Activity activity = activityRepository.save(newActivity(user, "A-1001", "2026-04"));

        SelectAlbumActivitiesResponse first = albumService.selectAlbumActivities(
                user.getId(),
                album.getId(),
                new SelectAlbumActivitiesRequest(List.of(activity.getId()))
        );
        SelectAlbumActivitiesResponse second = albumService.selectAlbumActivities(
                user.getId(),
                album.getId(),
                new SelectAlbumActivitiesRequest(List.of(activity.getId()))
        );

        assertThat(first.addedCount()).isEqualTo(1);
        assertThat(first.skippedCount()).isEqualTo(0);
        assertThat(second.addedCount()).isEqualTo(0);
        assertThat(second.skippedCount()).isEqualTo(1);
    }

    @Test
    void 활동_선택해제시_연결된_사진도_함께_삭제된다() {
        User user = userRepository.save(newUser("album-test-delete@sweetbook.com"));
        AlbumProject album = albumProjectRepository.save(newAlbum(user, "2026-04"));
        Activity activity = activityRepository.save(newActivity(user, "A-2001", "2026-04"));

        albumService.selectAlbumActivities(
                user.getId(),
                album.getId(),
                new SelectAlbumActivitiesRequest(List.of(activity.getId()))
        );

        AlbumActivity albumActivity = albumActivityRepository
                .findByAlbumProjectIdAndActivityId(album.getId(), activity.getId())
                .orElseThrow();

        activityPhotoRepository.save(ActivityPhoto.builder()
                .albumActivity(albumActivity)
                .originalFileName("x.png")
                .storedFileName("x.png")
                .contentType("image/png")
                .fileSize(10L)
                .storagePath("build/test-uploads/none.png")
                .build());

        DeselectAlbumActivityResponse response = albumService.deselectAlbumActivity(
                user.getId(),
                album.getId(),
                activity.getId()
        );

        assertThat(response.deleted()).isTrue();
        assertThat(response.selectedActivityCount()).isZero();
        assertThat(albumActivityRepository.findByAlbumProjectIdAndActivityId(album.getId(), activity.getId())).isEmpty();
        assertThat(activityPhotoRepository.countByAlbumActivityId(albumActivity.getId())).isZero();
    }

    private User newUser(String email) {
        return User.builder()
                .email(email)
                .password("encoded-password")
                .role(UserRole.USER)
                .build();
    }

    private AlbumProject newAlbum(User user, String month) {
        return AlbumProject.builder()
                .user(user)
                .month(month)
                .title("test-album")
                .subtitle("sub")
                .monthlyReview("review")
                .status(AlbumProjectStatus.DRAFT)
                .build();
    }

    private Activity newActivity(User user, String externalId, String month) {
        LocalDateTime dateTime = YearMonth.parse(month).atDay(1).atStartOfDay();
        return Activity.builder()
                .user(user)
                .externalActivityId(externalId)
                .activityType("Run")
                .activityName("Morning Run")
                .activityDateTime(dateTime)
                .activityMonth(month)
                .distanceKm(5.0)
                .movingTimeSeconds(1500)
                .elapsedTimeSeconds(1600)
                .averageSpeed(3.3)
                .elevationGain(50.0)
                .calories(300)
                .build();
    }
}

