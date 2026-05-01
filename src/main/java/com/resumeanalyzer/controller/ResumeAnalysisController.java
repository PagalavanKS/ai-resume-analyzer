package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.AnalysisResponse;
import com.resumeanalyzer.dto.TextAnalysisRequest;
import com.resumeanalyzer.service.ResumeAnalysisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/resume")
public class ResumeAnalysisController {

    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeAnalysisController(ResumeAnalysisService resumeAnalysisService) {
        this.resumeAnalysisService = resumeAnalysisService;
    }

    @GetMapping("/health")
    public String health() {
        return "AI Resume Analyzer backend is running";
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResponse analyzeResumeFile(
            @RequestPart("resume") MultipartFile resume,
            @RequestParam("jobDescription") @NotBlank String jobDescription
    ) {
        return resumeAnalysisService.analyzeUploadedResume(resume, jobDescription);
    }

    @PostMapping(value = "/analyze-text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisResponse analyzeResumeText(@Valid @org.springframework.web.bind.annotation.RequestBody TextAnalysisRequest request) {
        return resumeAnalysisService.analyzeText(request.resumeText(), request.jobDescription());
    }
}
