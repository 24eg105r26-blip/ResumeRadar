package com.resumeradar.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ats_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtsIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_version_id", nullable = false)
    private ResumeVersion resumeVersion;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 500)
    private String description;
}
