package com.resumeradar.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeUploadDto {

    private Long resumeId;

    @NotBlank(message = "Resume title is required")
    private String resumeTitle;

    @NotBlank(message = "Extracted text cannot be empty")
    private String extractedText;

    private int healthScore;

    private Long jobDescriptionId;
    private String jobTitle;
    private String jobDescriptionText;
    private String jobExtractedSkills;

    private int matchPercentage;
    private String missingKeywords;
    private String matchedKeywords;

    @Builder.Default
    private List<AtsIssueDto> atsIssues = new ArrayList<>();
}
