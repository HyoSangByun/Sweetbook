package com.sweetbook.server.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Builder
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "activities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_activities_external", columnNames = {"external_activity_id"})
        },
        indexes = {
                @Index(name = "idx_activities_month", columnList = "activity_month"),
                @Index(name = "idx_activities_external_id", columnList = "external_activity_id")
        }
)
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_activity_id", nullable = false, length = 100)
    private String externalActivityId;

    @Column(nullable = false, length = 50)
    private String activityType;

    @Column(nullable = false, length = 200)
    private String activityName;

    @Column(length = 2000)
    private String description;

    @Column(name = "activity_date_time", nullable = false)
    private LocalDateTime activityDateTime;

    @Column(name = "activity_month", nullable = false, length = 7)
    private String activityMonth;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "moving_time_seconds")
    private Integer movingTimeSeconds;

    @Column(name = "elapsed_time_seconds")
    private Integer elapsedTimeSeconds;

    @Column(name = "average_speed")
    private Double averageSpeed;

    @Column(name = "elevation_gain")
    private Double elevationGain;

    @Column(name = "calories")
    private Integer calories;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

