package com.sweetbook.server.photo.repository;

import com.sweetbook.server.photo.domain.ActivityPhoto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityPhotoRepository extends JpaRepository<ActivityPhoto, Long> {

    Optional<ActivityPhoto> findByIdAndAlbumActivityId(Long id, Long albumActivityId);

    boolean existsByAlbumActivityAlbumProjectId(Long albumProjectId);

    void deleteByAlbumActivityId(Long albumActivityId);
}
