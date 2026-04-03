package com.sweetbook.server.activity.repository;

import com.sweetbook.server.activity.domain.Activity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query("""
            select distinct a.activityMonth
            from Activity a
            where a.user.id = :userId
            order by a.activityMonth desc
            """)
    List<String> findDistinctMonthsByUserId(@Param("userId") Long userId);

    List<Activity> findAllByUserIdAndActivityMonthOrderByActivityDateTimeDesc(Long userId, String activityMonth);

    Optional<Activity> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndExternalActivityId(Long userId, String externalActivityId);
}
