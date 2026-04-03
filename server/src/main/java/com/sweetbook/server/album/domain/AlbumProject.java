package com.sweetbook.server.album.domain;

import com.sweetbook.server.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "album_projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlbumProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "album_month", nullable = false, length = 7)
    private String month;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 300)
    private String subtitle;

    @Column(name = "monthly_review", length = 4000)
    private String monthlyReview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlbumProjectStatus status;

    @Column(name = "book_uid", length = 100)
    private String bookUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_status", nullable = false, length = 30)
    private BookGenerationStatus bookStatus;

    @Column(name = "book_generated_at")
    private LocalDateTime bookGeneratedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void update(String title, String subtitle, String monthlyReview) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (subtitle != null && !subtitle.isBlank()) {
            this.subtitle = subtitle;
        }
        if (monthlyReview != null && !monthlyReview.isBlank()) {
            this.monthlyReview = monthlyReview;
        }
    }

    public void markBookGenerated(String bookUid, LocalDateTime generatedAt) {
        this.bookUid = bookUid;
        this.bookStatus = BookGenerationStatus.GENERATED;
        this.bookGeneratedAt = generatedAt;
    }
}
