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
    private static final Set<String> EDUCATION_TERMS = Set.of(
            "bachelor", "master", "degree", "university", "college", "b.tech", "m.tech", "phd", "certification"
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
        Set<String> jobKeywords = extractKeywords(jobDescription);

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
        int educationScore = calculateSectionScore(resumeText, EDUCATION_TERMS);
        int overallScore = clamp(Math.round(skillsScore * 0.6f + experienceScore * 0.25f + educationScore * 0.15f));

        List<String> strengths = buildStrengths(matchedKeywords, experienceScore, educationScore);
        List<String> suggestions = buildSuggestions(missingKeywords, experienceScore, educationScore, resumeText);

        return new AnalysisResponse(
                overallScore,
                skillsScore,
                experienceScore,
                educationScore,
                matchedKeywords,
                missingKeywords,
                strengths,
                suggestions,
                buildSummary(overallScore)
        );
    }

    private Set<String> extractKeywords(String text) {
        Map<String, Long> frequencies = WORD_PATTERN.matcher(text.toLowerCase(Locale.ROOT))
                .results()
                .map(match -> normalize(match.group()))
                .filter(word -> word.length() > 2)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()));

        return frequencies.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(60)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String word) {
        return word.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9+#.\\-]+$", "");
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

    private List<String> buildStrengths(List<String> matchedKeywords, int experienceScore, int educationScore) {
        List<String> strengths = new ArrayList<>();
        if (!matchedKeywords.isEmpty()) {
            strengths.add("Matches important job keywords: " + String.join(", ", matchedKeywords.stream().limit(8).toList()));
        }
        if (experienceScore >= 50) {
            strengths.add("Shows relevant project or work experience signals");
        }
        if (educationScore >= 40) {
            strengths.add("Includes education or certification details");
        }
        if (strengths.isEmpty()) {
            strengths.add("Resume text was parsed successfully and is ready for targeted improvement");
        }
        return strengths;
    }

    private List<String> buildSuggestions(List<String> missingKeywords, int experienceScore, int educationScore, String resumeText) {
        List<String> suggestions = new ArrayList<>();
        if (!missingKeywords.isEmpty()) {
            suggestions.add("Add truthful examples using these job keywords: " + String.join(", ", missingKeywords.stream().limit(10).toList()));
        }
        if (experienceScore < 50) {
            suggestions.add("Add measurable project outcomes, responsibilities, and tools used in each role");
        }
        if (educationScore < 40) {
            suggestions.add("Include education, certifications, or relevant coursework if applicable");
        }
        if (!containsMetric(resumeText)) {
            suggestions.add("Use numbers where possible, such as performance gains, users served, cost saved, or time reduced");
        }
        return deduplicate(suggestions);
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
