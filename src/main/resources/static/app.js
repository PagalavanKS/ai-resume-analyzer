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
    setClassName(fields.summary, "summary");
    setText(fields.summary, "Analyzing resume fit...");
    setText(fields.statusPill, "Analyzing");

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
        setClassName(fields.summary, "summary error");
        setText(fields.summary, error.message);
        setText(fields.statusPill, "Error");
    } finally {
        button.disabled = false;
        button.textContent = "Analyze Resume";
    }
});

function renderResults(data) {
    setText(fields.overallScore, `${data.overallScore}%`);
    setText(fields.skillsScore, `${data.skillsScore}%`);
    setText(fields.experienceScore, `${data.experienceScore}%`);
    setText(fields.summary, data.summary);
    setText(fields.statusPill, "Complete");

    renderList(fields.matchedKeywords, data.matchedKeywords);
    renderList(fields.missingKeywords, data.missingKeywords);
    renderList(fields.strengths, data.strengths);
    renderList(fields.suggestions, data.suggestions);
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
