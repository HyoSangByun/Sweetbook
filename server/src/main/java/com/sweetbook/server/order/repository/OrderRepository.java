package com.sweetbook.server.order.repository;

import com.sweetbook.server.order.domain.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByAlbumProjectIdAndExternalRef(Long albumProjectId, String externalRef);

    List<Order> findAllByAlbumProjectIdOrderByCreatedAtDesc(Long albumProjectId);

    List<Order> findAllByOrderByCreatedAtDesc();

    Optional<Order> findByIdAndAlbumProjectId(Long id, Long albumProjectId);

    Optional<Order> findByOrderUid(String orderUid);
}
