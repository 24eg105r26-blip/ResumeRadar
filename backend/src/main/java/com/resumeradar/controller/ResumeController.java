package com.resumeradar.controller;

import com.resumeradar.dto.ResumeUploadDto;
import com.resumeradar.dto.ScoreTrendDto;
import com.resumeradar.entity.Resume;
import com.resumeradar.entity.ResumeVersion;
import com.resumeradar.entity.User;
import com.resumeradar.security.SecurityUtils;
import com.resumeradar.service.MentorService;
import com.resumeradar.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ResumeController {

    private final SecurityUtils securityUtils;
    private final ResumeService resumeService;
    private final MentorService mentorService;

    @GetMapping("/resumes")
    public String listResumes(Model model) {
        User user = securityUtils.getRequiredCurrentUser();
        List<Resume> resumes = resumeService.getUserResumes(user);
        model.addAttribute("resumes", resumes);
        return "resumes/list";
    }

    @GetMapping("/resumes/{id}")
    public String viewResume(@PathVariable("id") Long id, Model model) {
        User user = securityUtils.getRequiredCurrentUser();
        Resume resume = resumeService.getResume(id, user);
        List<ScoreTrendDto> scoreTrends = resumeService.getScoreTrend(id, user);

        model.addAttribute("resume", resume);
        model.addAttribute("versions", resume.getVersions());
        model.addAttribute("latestVersion", resume.getLatestVersion());
        model.addAttribute("scoreTrends", scoreTrends);

        return "resumes/detail";
    }

    @GetMapping("/resumes/versions/{versionId}")
    public String viewVersion(@PathVariable("versionId") Long versionId, Model model) {
        User user = securityUtils.getRequiredCurrentUser();
        ResumeVersion version = resumeService.getResumeVersion(versionId, user);
        model.addAttribute("version", version);
        model.addAttribute("resume", version.getResume());
        model.addAttribute("atsIssues", version.getAtsIssues());
        model.addAttribute("matchReports", version.getMatchReports());
        model.addAttribute("comments", mentorService.getCommentsForVersion(versionId, user));
        return "resumes/version-detail";
    }

    @PostMapping("/api/resumes/upload")
    @ResponseBody
    public ResponseEntity<?> handleOnDeviceUpload(@Valid @RequestBody ResumeUploadDto uploadDto) {
        User user = securityUtils.getRequiredCurrentUser();
        try {
            ResumeVersion savedVersion = resumeService.saveResumeVersion(user, uploadDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resumeId", savedVersion.getResume().getId());
            response.put("versionId", savedVersion.getId());
            response.put("versionNumber", savedVersion.getVersionNumber());
            response.put("healthScore", savedVersion.getHealthScore());
            response.put("scoreBand", savedVersion.getHealthScoreBand());
            response.put("redirectUrl", "/dashboard?resumeId=" + savedVersion.getResume().getId());

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/resumes/{id}/delete")
    public String deleteResume(@PathVariable("id") Long id) {
        User user = securityUtils.getRequiredCurrentUser();
        resumeService.deleteResume(id, user);
        return "redirect:/dashboard";
    }
}
