package com.sweetbook.server.activity.repository;

import com.sweetbook.server.activity.domain.Activity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query("""
            select distinct a.activityMonth
            from Activity a
            order by a.activityMonth desc
            """)
    List<String> findDistinctMonths();

    List<Activity> findAllByActivityMonthOrderByActivityDateTimeDesc(String activityMonth);

    Optional<Activity> findById(Long id);

    boolean existsByExternalActivityId(String externalActivityId);
}
