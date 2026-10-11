package com.resumeradar.service;

import com.resumeradar.dto.UserSummaryDto;
import com.resumeradar.entity.User;
import com.resumeradar.repository.MentorLinkRepository;
import com.resumeradar.repository.ResumeRepository;
import com.resumeradar.repository.ResumeVersionRepository;
import com.resumeradar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final MentorLinkRepository mentorLinkRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalResumes", resumeRepository.count());
        stats.put("totalVersions", resumeVersionRepository.count());
        stats.put("totalMentorLinks", mentorLinkRepository.count());
        return stats;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserSummaryDto> summaries = new ArrayList<>();
        for (User u : users) {
            String roleNames = u.getRoles().stream()
                    .map(r -> r.getName().replace("ROLE_", ""))
                    .collect(Collectors.joining(", "));
            long resumeCount = resumeRepository.countByUser(u);
            summaries.add(UserSummaryDto.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .email(u.getEmail())
                    .roles(roleNames)
                    .createdAt(u.getCreatedAt())
                    .resumeCount(resumeCount)
                    .build());
        }
        return summaries;
    }
}
