package com.resumeanalyzer.dto;

import jakarta.validation.constraints.NotBlank;

public record TextAnalysisRequest(
        @NotBlank(message = "Resume text is required")
        String resumeText,

        @NotBlank(message = "Job description is required")
        String jobDescription
) {
}
