package com.resumeanalyzer.dto;

import java.util.List;

public record AnalysisResponse(
        int overallScore,
        int atsScore,
        int skillsScore,
        int experienceScore,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        List<String> strengths,
        List<String> suggestions,
        List<String> aiSuggestions,
        List<String> atsFindings,
        String summary
) {
}
