package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.AnalysisResponse;
import com.resumeanalyzer.exception.ResumeAnalysisException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResumeAnalysisService {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z+#.\\-]{1,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "about", "above", "after", "again", "against", "also", "and", "any", "are", "because", "been",
            "being", "between", "both", "can", "did", "does", "doing", "down", "each", "few", "for", "from",
            "has", "have", "having", "here", "into", "its", "more", "most", "our", "out", "over", "own",
            "same", "she", "should", "some", "such", "than", "that", "the", "their", "then", "there",
            "these", "they", "this", "through", "too", "under", "until", "very", "was", "were", "what",
            "when", "where", "which", "while", "who", "why", "will", "with", "you", "your"
    );
    private static final Set<String> LOW_SIGNAL_KEYWORDS = Set.of(
            "ability", "analytical", "application", "applications", "assist", "attention", "bachelor", "based", "building",
            "clean", "code", "communication", "cross-functional", "cycles", "define", "degree", "excellent",
            "fast", "good", "high", "including", "knowledge", "learn", "multiple", "preferred", "problem",
            "problems", "process", "provide", "related", "required", "requirements", "responsibilities",
            "role", "skills", "solutions", "strong", "support", "team", "teams", "using", "work", "working"
    );
    private static final Set<String> MAJOR_KEYWORDS = Set.of(
            "agile", "ai", "angular", "ansible", "api", "apis", "automation", "aws", "azure",
            "backend", "boot", "bootstrap", "c", "c#", "c++", "ci", "ci/cd", "cloud", "css",
            "database", "databases", "debugging", "deployment", "devops", "django", "docker",
            "ec2", "express", "fastapi", "figma", "flask", "frontend", "git", "github",
            "gitlab", "golang", "graphql", "hibernate", "html", "java", "javascript", "jenkins",
            "jira", "jquery", "json", "junit", "kafka", "kotlin", "kubernetes", "lambda",
            "linux", "machine", "microservices", "mongodb", "mvc", "mysql", "next.js", "node",
            "node.js", "nosql", "oauth", "oracle", "postgresql", "python", "react", "redis",
            "rest", "restful", "ruby", "s3", "scrum", "security", "selenium", "spring",
            "springboot", "spring-boot", "sql", "tailwind", "testing", "typescript", "ui",
            "unit", "ux", "vue"
    );
    private static final Set<String> EXPERIENCE_TERMS = Set.of(
            "experience", "worked", "built", "developed", "managed", "led", "delivered", "implemented",
            "designed", "optimized", "deployed", "maintained", "improved", "reduced", "increased"
    );

    private final ResumeTextExtractor resumeTextExtractor;

    public ResumeAnalysisService(ResumeTextExtractor resumeTextExtractor) {
        this.resumeTextExtractor = resumeTextExtractor;
    }

    public AnalysisResponse analyzeUploadedResume(MultipartFile resume, String jobDescription) {
        return analyzeText(resumeTextExtractor.extract(resume), jobDescription);
    }

    public AnalysisResponse analyzeText(String resumeText, String jobDescription) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new ResumeAnalysisException("Resume text is required");
        }
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new ResumeAnalysisException("Job description is required");
        }

        Set<String> resumeKeywords = extractKeywords(resumeText);
        Set<String> jobKeywords = extractMajorKeywords(jobDescription);

        List<String> matchedKeywords = jobKeywords.stream()
                .filter(resumeKeywords::contains)
                .sorted()
                .toList();

        List<String> missingKeywords = jobKeywords.stream()
                .filter(keyword -> !resumeKeywords.contains(keyword))
                .sorted()
                .limit(20)
                .toList();

        int skillsScore = calculateKeywordScore(matchedKeywords.size(), jobKeywords.size());
        int experienceScore = calculateSectionScore(resumeText, EXPERIENCE_TERMS);
        int overallScore = clamp(Math.round(skillsScore * 0.75f + experienceScore * 0.25f));
        int atsScore = calculateAtsScore(resumeText, skillsScore, experienceScore, matchedKeywords.size());

        List<String> strengths = buildStrengths(matchedKeywords, experienceScore);
        List<String> suggestions = buildSuggestions(missingKeywords, experienceScore, resumeText);
        List<String> aiSuggestions = buildAiSuggestions(missingKeywords, experienceScore, resumeText);
        List<String> atsFindings = buildAtsFindings(resumeText, matchedKeywords.size(), missingKeywords.size());

        return new AnalysisResponse(
                overallScore,
                atsScore,
                skillsScore,
                experienceScore,
                matchedKeywords,
                missingKeywords,
                strengths,
                suggestions,
                aiSuggestions,
                atsFindings,
                buildSummary(overallScore)
        );
    }

    private Set<String> extractKeywords(String text) {
        Map<String, Long> frequencies = WORD_PATTERN.matcher(text.toLowerCase(Locale.ROOT))
                .results()
                .map(match -> normalize(match.group()))
                .filter(word -> word.length() > 2)
                .filter(word -> !STOP_WORDS.contains(word))
                .filter(word -> !LOW_SIGNAL_KEYWORDS.contains(word))
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()));

        return frequencies.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(60)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> extractMajorKeywords(String text) {
        Set<String> extractedKeywords = extractKeywords(text);
        Set<String> majorKeywords = extractedKeywords.stream()
                .filter(MAJOR_KEYWORDS::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!majorKeywords.isEmpty()) {
            return majorKeywords;
        }

        return extractedKeywords.stream()
                .limit(12)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String word) {
        return word.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9+#]+$", "");
    }

    private int calculateKeywordScore(int matchedCount, int totalCount) {
        if (totalCount == 0) {
            return 0;
        }
        return clamp(Math.round((matchedCount * 100.0f) / totalCount));
    }

    private int calculateSectionScore(String resumeText, Set<String> terms) {
        String normalizedText = resumeText.toLowerCase(Locale.ROOT);
        long hits = terms.stream().filter(normalizedText::contains).count();
        int score = Math.round((hits * 100.0f) / terms.size());
        return clamp(score);
    }

    private List<String> buildStrengths(List<String> matchedKeywords, int experienceScore) {
        List<String> strengths = new ArrayList<>();
        if (!matchedKeywords.isEmpty()) {
            strengths.add("Matches important job keywords: " + String.join(", ", matchedKeywords.stream().limit(8).toList()));
        }
        if (experienceScore >= 50) {
            strengths.add("Shows relevant project or work experience signals");
        }
        if (strengths.isEmpty()) {
            strengths.add("Resume text was parsed successfully and is ready for targeted improvement");
        }
        return strengths;
    }

    private List<String> buildSuggestions(List<String> missingKeywords, int experienceScore, String resumeText) {
        List<String> suggestions = new ArrayList<>();
        if (!missingKeywords.isEmpty()) {
            suggestions.add("Add truthful examples using these job keywords: " + String.join(", ", missingKeywords.stream().limit(10).toList()));
        }
        if (experienceScore < 50) {
            suggestions.add("Add measurable project outcomes, responsibilities, and tools used in each role");
        }
        if (!containsMetric(resumeText)) {
            suggestions.add("Use numbers where possible, such as performance gains, users served, cost saved, or time reduced");
        }
        return deduplicate(suggestions);
    }

    private int calculateAtsScore(String resumeText, int skillsScore, int experienceScore, int matchedKeywordCount) {
        int metricScore = containsMetric(resumeText) ? 100 : 35;
        int lengthScore = calculateResumeLengthScore(resumeText);
        int keywordPresenceScore = matchedKeywordCount >= 5 ? 100 : matchedKeywordCount * 20;

        return clamp(Math.round(
                skillsScore * 0.45f +
                        experienceScore * 0.25f +
                        metricScore * 0.15f +
                        lengthScore * 0.10f +
                        keywordPresenceScore * 0.05f
        ));
    }

    private int calculateResumeLengthScore(String resumeText) {
        long wordCount = WORD_PATTERN.matcher(resumeText).results().count();
        if (wordCount < 80) {
            return 35;
        }
        if (wordCount <= 900) {
            return 100;
        }
        return 70;
    }

    private List<String> buildAiSuggestions(List<String> missingKeywords, int experienceScore, String resumeText) {
        List<String> aiSuggestions = new ArrayList<>();
        if (!missingKeywords.isEmpty()) {
            aiSuggestions.add("Rewrite one or two project bullets to naturally include: " + String.join(", ", missingKeywords.stream().limit(5).toList()) + ".");
        }
        if (experienceScore < 50) {
            aiSuggestions.add("Start bullets with action verbs like built, implemented, optimized, deployed, or improved.");
        }
        if (!containsMetric(resumeText)) {
            aiSuggestions.add("Add measurable impact, for example response time reduced by 30%, handled 1,000 users, or automated 5 hours of work weekly.");
        }
        aiSuggestions.add("Keep the resume ATS-friendly with clear headings, simple formatting, and role-specific technical keywords.");
        return deduplicate(aiSuggestions);
    }

    private List<String> buildAtsFindings(String resumeText, int matchedKeywordCount, int missingKeywordCount) {
        List<String> findings = new ArrayList<>();
        findings.add(matchedKeywordCount >= 5
                ? "Good keyword coverage for the target role"
                : "Keyword coverage is low; add more relevant technical terms from the job description");
        findings.add(containsMetric(resumeText)
                ? "Includes measurable achievements"
                : "Add numbers or percentages to improve ATS and recruiter readability");
        findings.add(missingKeywordCount == 0
                ? "No major missing keywords detected"
                : "Some major job keywords are still missing");
        findings.add(calculateResumeLengthScore(resumeText) >= 70
                ? "Resume length looks acceptable for ATS parsing"
                : "Resume text is short; add more project, responsibility, and impact details");
        return findings;
    }

    private boolean containsMetric(String text) {
        return Pattern.compile("\\d+%?|\\$\\d+|\\d+\\+").matcher(text).find();
    }

    private List<String> deduplicate(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private String buildSummary(int overallScore) {
        if (overallScore >= 80) {
            return "Strong match. The resume aligns well with the job description and needs only focused polishing.";
        }
        if (overallScore >= 60) {
            return "Moderate match. The resume has relevant signals but should be tailored with stronger keyword coverage and evidence.";
        }
        return "Needs tailoring. Add role-specific skills, measurable achievements, and clearer experience details before applying.";
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
