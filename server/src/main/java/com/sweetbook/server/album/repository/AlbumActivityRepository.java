package com.sweetbook.server.album.repository;

import com.sweetbook.server.album.domain.AlbumActivity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlbumActivityRepository extends JpaRepository<AlbumActivity, Long> {

    List<AlbumActivity> findAllByAlbumProjectIdOrderByActivityActivityDateTimeDesc(Long albumProjectId);

    boolean existsByAlbumProjectIdAndActivityId(Long albumProjectId, Long activityId);

    long countByAlbumProjectId(Long albumProjectId);

    Optional<AlbumActivity> findByAlbumProjectIdAndActivityId(Long albumProjectId, Long activityId);

    @Modifying
    @Query(
            value = """
                    insert into album_activities (album_project_id, activity_id, memo, created_at)
                    select :albumProjectId, :activityId, null, current_timestamp
                    where not exists (
                        select 1
                        from album_activities aa
                        where aa.album_project_id = :albumProjectId
                          and aa.activity_id = :activityId
                    )
                    """,
            nativeQuery = true
    )
    int insertAlbumActivity(
            @Param("albumProjectId") Long albumProjectId,
            @Param("activityId") Long activityId
    );
}
