package com.resumeanalyzer.service;

import com.resumeanalyzer.exception.ResumeAnalysisException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Component
public class ResumeTextExtractor {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    private final Tika tika = new Tika();

    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeAnalysisException("Resume file is required");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ResumeAnalysisException("Unsupported file type. Upload PDF, DOC, DOCX, or TXT");
        }

        try {
            String text = tika.parseToString(file.getInputStream());
            if (text == null || text.isBlank()) {
                throw new ResumeAnalysisException("Could not extract text from resume");
            }
            return text;
        } catch (IOException exception) {
            throw new ResumeAnalysisException("Could not read resume file", exception);
        } catch (Exception exception) {
            throw new ResumeAnalysisException("Could not parse resume file", exception);
        }
    }
}
