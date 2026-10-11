package com.resumeradar.controller;

import com.resumeradar.dto.MentorLinkDto;
import com.resumeradar.dto.ReviewCommentDto;
import com.resumeradar.dto.ScoreTrendDto;
import com.resumeradar.entity.MentorLink;
import com.resumeradar.entity.User;
import com.resumeradar.security.SecurityUtils;
import com.resumeradar.service.MentorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MentorController {

    private final SecurityUtils securityUtils;
    private final MentorService mentorService;

    @GetMapping("/mentor/dashboard")
    @PreAuthorize("hasAnyRole('MENTOR', 'ADMIN')")
    public String mentorDashboard(Model model) {
        User mentor = securityUtils.getRequiredCurrentUser();
        List<MentorLink> linkedLearners = mentorService.getLinkedLearners(mentor);

        model.addAttribute("mentor", mentor);
        model.addAttribute("linkedLearners", linkedLearners);
        if (!model.containsAttribute("linkDto")) {
            model.addAttribute("linkDto", new MentorLinkDto());
        }
        return "mentor/dashboard";
    }

    @PostMapping("/mentor/link")
    @PreAuthorize("hasAnyRole('MENTOR', 'ADMIN')")
    public String linkLearner(@Valid @ModelAttribute("linkDto") MentorLinkDto linkDto,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Share code cannot be blank.");
            return "redirect:/mentor/dashboard";
        }
        User mentor = securityUtils.getRequiredCurrentUser();
        try {
            MentorLink link = mentorService.linkMentor(mentor, linkDto.getShareCode());
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Successfully linked with student: " + link.getLearner().getName());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/mentor/dashboard";
    }

    @GetMapping("/mentor/learner/{learnerId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'ADMIN')")
    public String viewLearnerTrends(@PathVariable("learnerId") Long learnerId, Model model) {
        User mentor = securityUtils.getRequiredCurrentUser();
        List<ScoreTrendDto> trends = mentorService.getLearnerScoreTrends(mentor, learnerId);

        model.addAttribute("mentor", mentor);
        model.addAttribute("learnerId", learnerId);
        model.addAttribute("trends", trends);
        model.addAttribute("commentDto", new ReviewCommentDto());

        return "mentor/learner-view";
    }

    @PostMapping("/mentor/comments")
    @PreAuthorize("hasAnyRole('MENTOR', 'ADMIN')")
    public String submitComment(@Valid @ModelAttribute("commentDto") ReviewCommentDto commentDto,
                                BindingResult bindingResult,
                                @RequestParam(value = "learnerId", required = false) Long learnerId,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Comment cannot be blank.");
            return "redirect:/mentor/learner/" + learnerId;
        }
        User mentor = securityUtils.getRequiredCurrentUser();
        try {
            mentorService.addReviewComment(mentor, commentDto);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback successfully posted to student!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/mentor/learner/" + (learnerId != null ? learnerId : "");
    }

    @PostMapping("/api/mentor/share-code/generate")
    @ResponseBody
    public ResponseEntity<?> generateShareCode() {
        User learner = securityUtils.getRequiredCurrentUser();
        String code = mentorService.generateShareCode(learner);
        Map<String, String> response = Collections.singletonMap("shareCode", code);
        return ResponseEntity.ok(response);
    }
}
