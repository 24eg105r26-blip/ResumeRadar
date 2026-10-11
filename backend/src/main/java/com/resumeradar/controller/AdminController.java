package com.resumeradar.controller;

import com.resumeradar.dto.UserSummaryDto;
import com.resumeradar.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public String adminDashboard(Model model) {
        Map<String, Object> stats = adminService.getSystemStatistics();
        List<UserSummaryDto> users = adminService.getAllUsers();

        model.addAttribute("stats", stats);
        model.addAttribute("users", users);

        return "admin/dashboard";
    }
}
