package com.resumeradar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthScoreServiceTest {

    private HealthScoreService healthScoreService;

    @BeforeEach
    void setUp() {
        healthScoreService = new HealthScoreService();
    }

    @Test
    @DisplayName("Should compute 100% health score when match is 100% and zero ATS issues")
    void testPerfectHealthScore() {
        int score = healthScoreService.calculateHealthScore(100, 0);
        assertEquals(100, score);
        assertEquals("Excellent", healthScoreService.getScoreBand(score));
    }

    @Test
    @DisplayName("Should blend 70% match and 30% ATS penalty accurately")
    void testBlendedHealthScore() {
        // match = 50 -> 50 * 0.7 = 35.0
        // ats issues = 2 -> 30 - (2 * 6) = 18.0
        // total = 35 + 18 = 53
        int score = healthScoreService.calculateHealthScore(50, 2);
        assertEquals(53, score);
        assertEquals("Needs Work", healthScoreService.getScoreBand(score));
    }

    @Test
    @DisplayName("Should handle excessive ATS penalties gracefully without going below 0")
    void testSevereAtsPenalties() {
        // match = 0, ats issues = 10 -> ats component floored at 0
        int score = healthScoreService.calculateHealthScore(0, 10);
        assertEquals(0, score);
        assertEquals("Weak", healthScoreService.getScoreBand(score));
    }

    @Test
    @DisplayName("Should correctly classify health score bands")
    void testHealthScoreBands() {
        assertEquals("Excellent", healthScoreService.getScoreBand(85));
        assertEquals("Good", healthScoreService.getScoreBand(75));
        assertEquals("Needs Work", healthScoreService.getScoreBand(45));
        assertEquals("Weak", healthScoreService.getScoreBand(20));
    }
}
