package com.resumeradar.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreTrendDto {
    private Long versionId;
    private int versionNumber;
    private int healthScore;
    private String scoreBand;
    private LocalDateTime createdAt;
    private int atsIssueCount;
    private int commentCount;
}
