const form = document.querySelector("#analysis-form");
const button = document.querySelector("#analyzeButton");
const downloadReportButton = document.querySelector("#downloadReportButton");
const resumeFile = document.querySelector("#resumeFile");
const fileName = document.querySelector("#fileName");
let latestAnalysis = null;

const fields = {
    overallScore: document.querySelector("#overallScore"),
    atsScore: document.querySelector("#atsScore"),
    skillsScore: document.querySelector("#skillsScore"),
    experienceScore: document.querySelector("#experienceScore"),
    summary: document.querySelector("#summary"),
    matchedKeywords: document.querySelector("#matchedKeywords"),
    missingKeywords: document.querySelector("#missingKeywords"),
    strengths: document.querySelector("#strengths"),
    suggestions: document.querySelector("#suggestions"),
    aiSuggestions: document.querySelector("#aiSuggestions"),
    atsFindings: document.querySelector("#atsFindings"),
    statusPill: document.querySelector("#statusPill")
};

resumeFile.addEventListener("change", () => {
    setText(fileName, resumeFile.files[0]?.name || "PDF, DOC, DOCX, or TXT");
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    button.disabled = true;
    downloadReportButton.disabled = true;
    button.textContent = "Analyzing...";
    setClassName(fields.summary, "summary");
    setText(fields.summary, "Analyzing resume fit...");
    setText(fields.statusPill, "Analyzing");

    try {
        const response = await submitAnalysis();

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.details?.join(" ") || "Analysis failed");
        }

        renderResults(data);
        latestAnalysis = data;
        downloadReportButton.disabled = false;
    } catch (error) {
        setClassName(fields.summary, "summary error");
        setText(fields.summary, error.message);
        setText(fields.statusPill, "Error");
    } finally {
        button.disabled = false;
        button.textContent = "Analyze Resume";
    }
});

downloadReportButton.addEventListener("click", () => {
    if (latestAnalysis) {
        openPrintableReport(latestAnalysis);
    }
});

function submitAnalysis() {
    const file = resumeFile.files[0];
    if (file) {
        const formData = new FormData();
        formData.append("resume", file);
        formData.append("jobDescription", form.jobDescription.value);

        return fetch("/api/resume/analyze", {
            method: "POST",
            body: formData
        });
    }

    if (!form.resumeText.value.trim()) {
        throw new Error("Paste resume text or upload a resume file.");
    }

    return fetch("/api/resume/analyze-text", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            resumeText: form.resumeText.value,
            jobDescription: form.jobDescription.value
        })
    });
}

function renderResults(data) {
    setText(fields.overallScore, `${data.overallScore}%`);
    setText(fields.atsScore, `${data.atsScore}%`);
    setText(fields.skillsScore, `${data.skillsScore}%`);
    setText(fields.experienceScore, `${data.experienceScore}%`);
    setText(fields.summary, data.summary);
    setText(fields.statusPill, "Complete");

    renderList(fields.matchedKeywords, data.matchedKeywords);
    renderList(fields.missingKeywords, data.missingKeywords);
    renderList(fields.strengths, data.strengths);
    renderList(fields.suggestions, data.suggestions);
    renderList(fields.aiSuggestions, data.aiSuggestions);
    renderList(fields.atsFindings, data.atsFindings);
}

function renderList(element, values) {
    if (!element) {
        return;
    }

    element.innerHTML = "";
    if (!values || values.length === 0) {
        const item = document.createElement("li");
        item.textContent = "None";
        element.appendChild(item);
        return;
    }

    values.forEach((value) => {
        const item = document.createElement("li");
        item.textContent = value;
        element.appendChild(item);
    });
}

function setText(element, value) {
    if (element) {
        element.textContent = value;
    }
}

function setClassName(element, value) {
    if (element) {
        element.className = value;
    }
}

function openPrintableReport(data) {
    const reportWindow = window.open("", "_blank");
    if (!reportWindow) {
        setClassName(fields.summary, "summary error");
        setText(fields.summary, "Allow popups to download the PDF report.");
        return;
    }

    reportWindow.document.write(`
        <!doctype html>
        <html lang="en">
        <head>
            <meta charset="utf-8">
            <title>AI Resume Analyzer Report</title>
            <style>
                body { font-family: Arial, sans-serif; color: #18202a; padding: 32px; line-height: 1.5; }
                h1 { margin-bottom: 4px; }
                .scores { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin: 24px 0; }
                .score { border: 1px solid #dce2ea; padding: 14px; border-radius: 8px; }
                .score strong { display: block; font-size: 28px; color: #155e75; }
                section { margin-top: 22px; }
                ul { padding-left: 20px; }
                @media print { button { display: none; } body { padding: 0; } }
            </style>
        </head>
        <body>
            <button onclick="window.print()">Save as PDF</button>
            <h1>AI Resume Analyzer Report</h1>
            <p>${escapeHtml(data.summary)}</p>
            <div class="scores">
                ${scoreCard("Overall", data.overallScore)}
                ${scoreCard("ATS", data.atsScore)}
                ${scoreCard("Skills", data.skillsScore)}
                ${scoreCard("Experience", data.experienceScore)}
            </div>
            ${reportSection("Matched Keywords", data.matchedKeywords)}
            ${reportSection("Missing Keywords", data.missingKeywords)}
            ${reportSection("Strengths", data.strengths)}
            ${reportSection("Suggestions", data.suggestions)}
            ${reportSection("AI Suggestions", data.aiSuggestions)}
            ${reportSection("ATS Findings", data.atsFindings)}
        </body>
        </html>
    `);
    reportWindow.document.close();
    reportWindow.focus();
    reportWindow.print();
}

function scoreCard(label, value) {
    return `<div class="score"><span>${label}</span><strong>${value}%</strong></div>`;
}

function reportSection(title, values) {
    const items = (values && values.length ? values : ["None"])
        .map((value) => `<li>${escapeHtml(value)}</li>`)
        .join("");
    return `<section><h2>${title}</h2><ul>${items}</ul></section>`;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
