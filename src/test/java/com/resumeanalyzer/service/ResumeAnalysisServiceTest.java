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

    @Test
    void ignoresGenericKeywordsWhenFindingGaps() {
        String resume = "Azure cloud developer with databases debugging and deployment experience.";
        String job = """
                Strong analytical ability and attention to clean code.
                Bachelor degree preferred.
                Azure cloud databases debugging deployment.
                """;

        AnalysisResponse response = service.analyzeText(resume, job);

        assertThat(response.missingKeywords())
                .doesNotContain("ability", "analytical", "attention", "bachelor", "clean", "code", "degree");
        assertThat(response.matchedKeywords())
                .contains("azure", "cloud", "databases", "debugging", "deployment");
        assertThat(response.skillsScore()).isGreaterThanOrEqualTo(80);
    }

    @Test
    void usesOnlyMajorJobDescriptionKeywordsForScoring() {
        String resume = "Java Spring Boot REST API developer with MySQL and AWS deployment experience.";
        String job = """
                We need a candidate with strong communication, analytical ability, clean code practices,
                attention to detail, cross-functional collaboration, and a bachelor degree.
                Main technical skills: Java, Spring Boot, REST API, MySQL, AWS, Docker, Kubernetes.
                """;

        AnalysisResponse response = service.analyzeText(resume, job);

        assertThat(response.missingKeywords())
                .doesNotContain("communication", "analytical", "ability", "attention", "bachelor", "degree");
        assertThat(response.missingKeywords()).contains("docker", "kubernetes");
        assertThat(response.matchedKeywords()).contains("java", "spring", "rest", "api", "mysql", "aws");
        assertThat(response.skillsScore()).isGreaterThanOrEqualTo(60);
    }
}
