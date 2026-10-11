package com.resumeradar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resume_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "health_score", nullable = false)
    private int healthScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "resumeVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AtsIssue> atsIssues = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "resumeVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchReport> matchReports = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "resumeVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<ReviewComment> reviewComments = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public String getHealthScoreBand() {
        if (healthScore >= 80) return "Excellent (Interview-ready)";
        if (healthScore >= 60) return "Good (Strong, minor gaps)";
        if (healthScore >= 40) return "Needs Work (Needs revision)";
        return "Weak (Major rework needed)";
    }

    public String getHealthScoreColorClass() {
        if (healthScore >= 80) return "badge-success";
        if (healthScore >= 60) return "badge-info";
        if (healthScore >= 40) return "badge-warning";
        return "badge-danger";
    }
}
