🚀 AI Resume Analyzer
🔗 Live Demo: https://ai-resume-analyzer-le6p.onrender.com/
📌 About
AI Resume Analyzer is a Spring Boot web application that helps users compare a resume with a job description. It analyzes resume text, checks keyword matches, identifies missing skills, calculates match scores, and gives practical suggestions to improve the resume for a target role.
The project includes both the frontend website and the Spring Boot backend in one application, so it can be deployed as a single service.
✨ Features
📝 Paste resume text and job description
📊 Get overall, skills, experience, and education scores
✅ View matched keywords
🔍 Find missing keywords from the job description
💡 Receive resume improvement suggestions
🌐 Frontend and backend served from one Spring Boot app
🐳 Docker-ready deployment
🛠️ Tech Stack
☕ Java 21
🍃 Spring Boot 3
🌐 Spring Web
✅ Jakarta Bean Validation
📄 Apache Tika
🎨 HTML, CSS, JavaScript
🐳 Docker
☁️ Render
🌍 Live Website
Open the hosted project here:
👉 AI Resume Analyzer Live
Health check endpoint:
GET https://ai-resume-analyzer-le6p.onrender.com/api/resume/health

📡 API Endpoints
Health Check
GET /api/resume/health

Analyze Text
POST /api/resume/analyze-text
Content-Type: application/json

Example request:
{
  "resumeText": "Java developer with Spring Boot, REST APIs, MySQL and AWS experience.",
  "jobDescription": "Looking for Java Spring Boot developer with REST API, AWS, Docker and MySQL skills."
}

Analyze Uploaded Resume
POST /api/resume/analyze
Content-Type: multipart/form-data

resume=<file>
jobDescription=<text>

Supported file types:
PDF
DOC
DOCX
TXT
▶️ Run Locally
mvn spring-boot:run

Then open:
http://localhost:8080

🧪 Run Tests
mvn test

🐳 Run With Docker
docker build -t ai-resume-analyzer .
docker run -p 8080:8080 ai-resume-analyzer

☁️ Deployment
This project is deployed on Render using Docker.
Render settings:
Environment: Docker
Health Check Path: /api/resume/health
Root Directory: empty

📁 Project Structure
src/main/java/com/resumeanalyzer
├── config
├── controller
├── dto
├── exception
└── service

src/main/resources/static
├── index.html
├── styles.css
└── app.js

👤 Author
Created as a full-stack Spring Boot project for resume analysis and job matching.
