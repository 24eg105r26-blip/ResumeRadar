package com.resumeradar.service;

import org.springframework.stereotype.Service;

@Service
public class HealthScoreService {

    /**
     * Blends the match percentage (70% weight) with a penalty for ATS formatting issues (30% weight).
     *
     * @param matchPercentage The keyword match percentage (0-100)
     * @param atsIssueCount   Number of detected ATS compatibility issues
     * @return Computed Health Score (0-100)
     */
    public int calculateHealthScore(int matchPercentage, int atsIssueCount) {
        int clampedMatch = Math.max(0, Math.min(100, matchPercentage));
        double matchWeightComponent = clampedMatch * 0.70;

        // Base ATS component is 30 points.
        // Each ATS issue deducts 6 points from the ATS score, floored at 0.
        int penaltyPerIssue = 6;
        double atsWeightComponent = Math.max(0, 30.0 - (atsIssueCount * penaltyPerIssue));

        int blendedScore = (int) Math.round(matchWeightComponent + atsWeightComponent);
        return Math.max(0, Math.min(100, blendedScore));
    }

    public String getScoreBand(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Needs Work";
        return "Weak";
    }

    public String getScoreBandDescription(int score) {
        if (score >= 80) return "Interview-ready: High keyword alignment and clean ATS structure.";
        if (score >= 60) return "Strong, minor gaps: Solid foundation with minor skill or formatting adjustments needed.";
        if (score >= 40) return "Needs revision: Critical keywords missing or formatting hurdles detected.";
        return "Major rework: Low job alignment or severe ATS formatting blockers.";
    }

    public String getScoreColor(int score) {
        if (score >= 80) return "#0d9488"; // Teal
        if (score >= 60) return "#0284c7"; // Blue
        if (score >= 40) return "#d97706"; // Amber
        return "#e11d48"; // Crimson
    }
}
