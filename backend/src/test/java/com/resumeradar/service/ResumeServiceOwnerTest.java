package com.resumeradar.service;

import com.resumeradar.dto.ResumeUploadDto;
import com.resumeradar.entity.Resume;
import com.resumeradar.entity.ResumeVersion;
import com.resumeradar.entity.User;
import com.resumeradar.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceOwnerTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ResumeVersionRepository resumeVersionRepository;
    @Mock
    private JobDescriptionRepository jobDescriptionRepository;
    @Mock
    private MatchReportRepository matchReportRepository;
    @Mock
    private AtsIssueRepository atsIssueRepository;
    @Mock
    private HealthScoreService healthScoreService;

    @InjectMocks
    private ResumeService resumeService;

    private User userA;
    private User userB;
    private Resume resumeOwnedByA;

    @BeforeEach
    void setUp() {
        userA = User.builder().id(1L).name("User A").email("a@test.com").build();
        userB = User.builder().id(2L).name("User B").email("b@test.com").build();

        resumeOwnedByA = Resume.builder()
                .id(100L)
                .user(userA)
                .title("A's Project")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Owner should successfully retrieve their own resume")
    void testOwnerCanAccessOwnResume() {
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resumeOwnedByA));

        Resume found = resumeService.getResume(100L, userA);
        assertNotNull(found);
        assertEquals(100L, found.getId());
    }

    @Test
    @DisplayName("Non-owner should be denied access when accessing another user's resume")
    void testNonOwnerCannotAccessOtherResume() {
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resumeOwnedByA));

        assertThrows(AccessDeniedException.class, () -> {
            resumeService.getResume(100L, userB);
        });
    }

    @Test
    @DisplayName("Non-owner should be denied when attempting to upload a version to another user's project")
    void testNonOwnerCannotAddVersionToOtherProject() {
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resumeOwnedByA));

        ResumeUploadDto dto = ResumeUploadDto.builder()
                .resumeId(100L)
                .resumeTitle("Hacked Project")
                .extractedText("some text")
                .matchPercentage(70)
                .build();

        assertThrows(AccessDeniedException.class, () -> {
            resumeService.saveResumeVersion(userB, dto);
        });
    }

    @Test
    @DisplayName("Owner should increment version number when saving an additional revision")
    void testVersionNumberIncrementsForOwner() {
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resumeOwnedByA));
        ResumeVersion existingV1 = ResumeVersion.builder()
                .id(10L)
                .resume(resumeOwnedByA)
                .versionNumber(1)
                .build();

        when(resumeVersionRepository.findTopByResumeOrderByVersionNumberDesc(resumeOwnedByA))
                .thenReturn(Optional.of(existingV1));
        when(healthScoreService.calculateHealthScore(anyInt(), anyInt())).thenReturn(75);
        when(resumeVersionRepository.save(any(ResumeVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResumeUploadDto dto = ResumeUploadDto.builder()
                .resumeId(100L)
                .resumeTitle("A's Project")
                .extractedText("Updated text for v2")
                .matchPercentage(80)
                .build();

        ResumeVersion savedV2 = resumeService.saveResumeVersion(userA, dto);
        assertNotNull(savedV2);
        assertEquals(2, savedV2.getVersionNumber(), "Next version number must be 2");
    }
}
