const form = document.querySelector("#analysis-form");
const button = document.querySelector("#analyzeButton");

const fields = {
    overallScore: document.querySelector("#overallScore"),
    skillsScore: document.querySelector("#skillsScore"),
    experienceScore: document.querySelector("#experienceScore"),
    summary: document.querySelector("#summary"),
    matchedKeywords: document.querySelector("#matchedKeywords"),
    missingKeywords: document.querySelector("#missingKeywords"),
    strengths: document.querySelector("#strengths"),
    suggestions: document.querySelector("#suggestions"),
    statusPill: document.querySelector("#statusPill")
};

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    button.disabled = true;
    button.textContent = "Analyzing...";
    fields.summary.className = "summary";
    fields.summary.textContent = "Analyzing resume fit...";
    fields.statusPill.textContent = "Analyzing";

    const payload = {
        resumeText: form.resumeText.value,
        jobDescription: form.jobDescription.value
    };

    try {
        const response = await fetch("/api/resume/analyze-text", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.details?.join(" ") || "Analysis failed");
        }

        renderResults(data);
    } catch (error) {
        fields.summary.className = "summary error";
        fields.summary.textContent = error.message;
        fields.statusPill.textContent = "Error";
    } finally {
        button.disabled = false;
        button.textContent = "Analyze Resume";
    }
});

function renderResults(data) {
    fields.overallScore.textContent = `${data.overallScore}%`;
    fields.skillsScore.textContent = `${data.skillsScore}%`;
    fields.experienceScore.textContent = `${data.experienceScore}%`;
    fields.summary.textContent = data.summary;
    fields.statusPill.textContent = "Complete";

    renderList(fields.matchedKeywords, data.matchedKeywords);
    renderList(fields.missingKeywords, data.missingKeywords);
    renderList(fields.strengths, data.strengths);
    renderList(fields.suggestions, data.suggestions);
}

function renderList(element, values) {
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
