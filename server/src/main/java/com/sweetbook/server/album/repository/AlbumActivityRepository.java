package com.sweetbook.server.album.repository;

import com.sweetbook.server.album.domain.AlbumActivity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumActivityRepository extends JpaRepository<AlbumActivity, Long> {

    List<AlbumActivity> findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(Long albumProjectId);

    boolean existsByAlbumProjectIdAndActivityId(Long albumProjectId, Long activityId);

    long countByAlbumProjectId(Long albumProjectId);

    Optional<AlbumActivity> findByAlbumProjectIdAndActivityId(Long albumProjectId, Long activityId);
}
