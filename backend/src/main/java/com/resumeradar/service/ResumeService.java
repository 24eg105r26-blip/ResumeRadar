package com.resumeradar.service;

import com.resumeradar.dto.AtsIssueDto;
import com.resumeradar.dto.ResumeUploadDto;
import com.resumeradar.dto.ScoreTrendDto;
import com.resumeradar.entity.*;
import com.resumeradar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final MatchReportRepository matchReportRepository;
    private final AtsIssueRepository atsIssueRepository;
    private final HealthScoreService healthScoreService;

    @Transactional
    public ResumeVersion saveResumeVersion(User user, ResumeUploadDto dto) {
        Resume resume;
        int nextVersionNumber = 1;

        if (dto.getResumeId() != null) {
            resume = resumeRepository.findById(dto.getResumeId())
                    .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + dto.getResumeId()));
            if (!resume.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("You do not own this resume project");
            }
            if (dto.getResumeTitle() != null && !dto.getResumeTitle().isBlank()) {
                resume.setTitle(dto.getResumeTitle().trim());
            }
            ResumeVersion latest = resumeVersionRepository.findTopByResumeOrderByVersionNumberDesc(resume).orElse(null);
            if (latest != null) {
                nextVersionNumber = latest.getVersionNumber() + 1;
            }
        } else {
            String title = (dto.getResumeTitle() != null && !dto.getResumeTitle().isBlank()) 
                    ? dto.getResumeTitle().trim() 
                    : "Resume Project - " + LocalDateTime.now().toLocalDate();
            resume = Resume.builder()
                    .user(user)
                    .title(title)
                    .createdAt(LocalDateTime.now())
                    .build();
            resume = resumeRepository.save(resume);
        }

        // Calculate verified Health Score on server
        int atsIssueCount = (dto.getAtsIssues() != null) ? dto.getAtsIssues().size() : 0;
        int computedHealthScore = healthScoreService.calculateHealthScore(dto.getMatchPercentage(), atsIssueCount);

        ResumeVersion version = ResumeVersion.builder()
                .resume(resume)
                .versionNumber(nextVersionNumber)
                .extractedText(dto.getExtractedText())
                .healthScore(computedHealthScore)
                .createdAt(LocalDateTime.now())
                .build();
        version = resumeVersionRepository.save(version);

        // Save ATS Issues
        if (dto.getAtsIssues() != null && !dto.getAtsIssues().isEmpty()) {
            List<AtsIssue> issues = new ArrayList<>();
            for (AtsIssueDto issueDto : dto.getAtsIssues()) {
                issues.add(AtsIssue.builder()
                        .resumeVersion(version)
                        .type(issueDto.getType() != null ? issueDto.getType() : "GENERAL")
                        .description(issueDto.getDescription())
                        .build());
            }
            atsIssueRepository.saveAll(issues);
            version.setAtsIssues(issues);
        }

        // Job Description and Match Report
        JobDescription jobDescription = null;
        if (dto.getJobDescriptionId() != null) {
            jobDescription = jobDescriptionRepository.findById(dto.getJobDescriptionId()).orElse(null);
        } else if (dto.getJobDescriptionText() != null && !dto.getJobDescriptionText().isBlank()) {
            String jdTitle = (dto.getJobTitle() != null && !dto.getJobTitle().isBlank())
                    ? dto.getJobTitle().trim()
                    : "Target Role - " + LocalDateTime.now().toLocalDate();
            jobDescription = JobDescription.builder()
                    .user(user)
                    .title(jdTitle)
                    .text(dto.getJobDescriptionText().trim())
                    .extractedSkills(dto.getJobExtractedSkills())
                    .createdAt(LocalDateTime.now())
                    .build();
            jobDescription = jobDescriptionRepository.save(jobDescription);
        }

        if (jobDescription != null || dto.getMatchPercentage() > 0 || dto.getMatchedKeywords() != null) {
            MatchReport report = MatchReport.builder()
                    .resumeVersion(version)
                    .jobDescription(jobDescription)
                    .matchPercentage(dto.getMatchPercentage())
                    .missingKeywords(dto.getMissingKeywords())
                    .matchedKeywords(dto.getMatchedKeywords())
                    .createdAt(LocalDateTime.now())
                    .build();
            matchReportRepository.save(report);
            version.getMatchReports().add(report);
        }

        return version;
    }

    @Transactional(readOnly = true)
    public List<Resume> getUserResumes(User user) {
        return resumeRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Resume getResume(Long id, User user) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + id));
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Unauthorized access to resume");
        }
        return resume;
    }

    @Transactional(readOnly = true)
    public ResumeVersion getResumeVersion(Long versionId, User user) {
        ResumeVersion version = resumeVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Resume version not found: " + versionId));
        if (!version.getResume().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Unauthorized access to resume version");
        }
        return version;
    }

    @Transactional(readOnly = true)
    public List<ScoreTrendDto> getScoreTrend(Long resumeId, User user) {
        Resume resume = getResume(resumeId, user);
        List<ResumeVersion> versions = resumeVersionRepository.findByResumeOrderByVersionNumberAsc(resume);
        List<ScoreTrendDto> trends = new ArrayList<>();
        for (ResumeVersion v : versions) {
            trends.add(ScoreTrendDto.builder()
                    .versionId(v.getId())
                    .versionNumber(v.getVersionNumber())
                    .healthScore(v.getHealthScore())
                    .scoreBand(healthScoreService.getScoreBand(v.getHealthScore()))
                    .createdAt(v.getCreatedAt())
                    .atsIssueCount(v.getAtsIssues() != null ? v.getAtsIssues().size() : 0)
                    .commentCount(v.getReviewComments() != null ? v.getReviewComments().size() : 0)
                    .build());
        }
        return trends;
    }

    @Transactional
    public void deleteResume(Long resumeId, User user) {
        Resume resume = getResume(resumeId, user);
        resumeRepository.delete(resume);
    }
}
