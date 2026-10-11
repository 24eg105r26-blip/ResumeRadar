package com.resumeradar.controller;

import com.resumeradar.dto.ScoreTrendDto;
import com.resumeradar.entity.JobDescription;
import com.resumeradar.entity.MentorLink;
import com.resumeradar.entity.Resume;
import com.resumeradar.entity.ResumeVersion;
import com.resumeradar.entity.User;
import com.resumeradar.security.SecurityUtils;
import com.resumeradar.service.JobDescriptionService;
import com.resumeradar.service.MentorService;
import com.resumeradar.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final SecurityUtils securityUtils;
    private final ResumeService resumeService;
    private final JobDescriptionService jobDescriptionService;
    private final MentorService mentorService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "resumeId", required = false) Long resumeId,
                            Model model) {
        User user = securityUtils.getRequiredCurrentUser();
        model.addAttribute("currentUser", user);

        List<Resume> resumes = resumeService.getUserResumes(user);
        model.addAttribute("resumes", resumes);

        List<JobDescription> jobDescriptions = jobDescriptionService.getUserJobDescriptions(user);
        model.addAttribute("jobDescriptions", jobDescriptions);

        // Selected or latest resume
        Resume selectedResume = null;
        if (resumeId != null) {
            selectedResume = resumes.stream()
                    .filter(r -> r.getId().equals(resumeId))
                    .findFirst()
                    .orElse(null);
        }
        if (selectedResume == null && !resumes.isEmpty()) {
            selectedResume = resumes.get(0);
        }
        model.addAttribute("selectedResume", selectedResume);

        List<ScoreTrendDto> scoreTrends = Collections.emptyList();
        ResumeVersion latestVersion = null;
        if (selectedResume != null) {
            scoreTrends = resumeService.getScoreTrend(selectedResume.getId(), user);
            latestVersion = selectedResume.getLatestVersion();
        }
        model.addAttribute("scoreTrends", scoreTrends);
        model.addAttribute("latestVersion", latestVersion);

        // Mentor share code
        List<MentorLink> mentorLinks = mentorService.getLearnerMentors(user);
        String shareCode = mentorLinks.isEmpty() ? null : mentorLinks.get(0).getShareCode();
        model.addAttribute("shareCode", shareCode);
        model.addAttribute("mentorLinks", mentorLinks);

        return "dashboard";
    }
}
