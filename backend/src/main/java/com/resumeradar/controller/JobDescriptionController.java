package com.resumeradar.controller;

import com.resumeradar.dto.JobDescriptionDto;
import com.resumeradar.entity.JobDescription;
import com.resumeradar.entity.User;
import com.resumeradar.security.SecurityUtils;
import com.resumeradar.service.JobDescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobDescriptionController {

    private final SecurityUtils securityUtils;
    private final JobDescriptionService jobDescriptionService;

    @GetMapping
    public String listJobs(Model model) {
        User user = securityUtils.getRequiredCurrentUser();
        List<JobDescription> jobs = jobDescriptionService.getUserJobDescriptions(user);
        model.addAttribute("jobs", jobs);
        return "jobs/list";
    }

    @GetMapping("/new")
    public String newJobForm(Model model) {
        if (!model.containsAttribute("jobDto")) {
            model.addAttribute("jobDto", new JobDescriptionDto());
        }
        return "jobs/form";
    }

    @GetMapping("/{id}")
    public String viewJob(@PathVariable("id") Long id, Model model) {
        User user = securityUtils.getRequiredCurrentUser();
        JobDescription jd = jobDescriptionService.getJobDescription(id, user);
        JobDescriptionDto dto = JobDescriptionDto.builder()
                .id(jd.getId())
                .title(jd.getTitle())
                .text(jd.getText())
                .extractedSkills(jd.getExtractedSkills())
                .build();
        model.addAttribute("jobDto", dto);
        model.addAttribute("isEdit", true);
        return "jobs/form";
    }

    @PostMapping("/save")
    public String saveJob(@Valid @ModelAttribute("jobDto") JobDescriptionDto jobDto,
                          BindingResult bindingResult,
                          Model model) {
        User user = securityUtils.getRequiredCurrentUser();
        if (bindingResult.hasErrors()) {
            return "jobs/form";
        }
        jobDescriptionService.saveJobDescription(user, jobDto);
        return "redirect:/jobs";
    }

    @PostMapping("/{id}/delete")
    public String deleteJob(@PathVariable("id") Long id) {
        User user = securityUtils.getRequiredCurrentUser();
        jobDescriptionService.deleteJobDescription(id, user);
        return "redirect:/jobs";
    }
}
