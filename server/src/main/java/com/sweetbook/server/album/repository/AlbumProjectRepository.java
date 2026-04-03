package com.sweetbook.server.album.repository;

import com.sweetbook.server.album.domain.AlbumProject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumProjectRepository extends JpaRepository<AlbumProject, Long> {

    Optional<AlbumProject> findByIdAndUserId(Long id, Long userId);
}

