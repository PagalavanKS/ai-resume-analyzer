# AI Resume Analyzer Backend

Spring Boot backend for analyzing a resume against a job description. It accepts either uploaded resume files or raw text, extracts keywords, scores the match, and returns actionable suggestions.

## Features

- Upload PDF, DOC, DOCX, or TXT resumes
- Analyze raw resume text through JSON
- Score overall fit, skills, experience, and education
- Return matched keywords, missing keywords, strengths, and improvement suggestions
- CORS enabled for local React/Vite frontends

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web
- Jakarta Bean Validation
- Apache Tika for resume text extraction

## API

### Health

```http
GET /api/resume/health
```

### Analyze Uploaded Resume

```http
POST /api/resume/analyze
Content-Type: multipart/form-data

resume=<file>
jobDescription=<text>
```

### Analyze Text

```http
POST /api/resume/analyze-text
Content-Type: application/json

{
  "resumeText": "Java developer with Spring Boot experience...",
  "jobDescription": "Looking for Java Spring Boot developer..."
}
```

## Run

```bash
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

## Test

```bash
mvn test
```

## Deploy

This project serves the frontend and backend from one Spring Boot app, so the deployed website opens at the service root URL.

### Render

1. Push this project to GitHub.
2. Create a new Render Web Service from that repository.
3. Choose Docker as the environment.
4. Use `/api/resume/health` as the health check path.
5. Deploy.

The included `render.yaml` can also be used as Render Blueprint configuration.

### Docker

```bash
docker build -t ai-resume-analyzer .
docker run -p 8080:8080 ai-resume-analyzer
```
