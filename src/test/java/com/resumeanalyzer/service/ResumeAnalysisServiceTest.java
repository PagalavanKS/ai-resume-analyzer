package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.AnalysisResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeAnalysisServiceTest {

    private final ResumeAnalysisService service = new ResumeAnalysisService(new ResumeTextExtractor());

    @Test
    void analyzesKeywordMatchesAndGaps() {
        String resume = """
                Java developer with Spring Boot experience.
                Built REST APIs, deployed applications, and improved latency by 30%.
                """;
        String job = "Looking for Java Spring Boot REST API developer with Docker and AWS experience.";

        AnalysisResponse response = service.analyzeText(resume, job);

        assertThat(response.overallScore()).isBetween(1, 100);
        assertThat(response.matchedKeywords()).contains("java", "spring", "boot");
        assertThat(response.missingKeywords()).contains("docker", "aws");
        assertThat(response.suggestions()).isNotEmpty();
    }
}
