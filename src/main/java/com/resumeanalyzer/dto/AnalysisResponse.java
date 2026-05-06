package com.resumeanalyzer.dto;

import java.util.List;

public record AnalysisResponse(
        int overallScore,
        int skillsScore,
        int experienceScore,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> strengths,
        List<String> suggestions,
        String summary
) {
}
