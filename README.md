# SFIP — Startup Failure Intelligence Platform

An AI-powered platform designed to analyze startup ideas, identify potential failure risks, and provide actionable insights to help entrepreneurs make more informed decisions.

## About the Project

The **Startup Failure Intelligence Platform (SFIP)** helps users evaluate startup ideas by generating structured AI-assisted analysis reports.

The platform aims to help aspiring entrepreneurs understand market challenges, business model viability, competitive threats, operational risks, and possible reasons a startup could fail.

SFIP currently uses the Groq API for AI-powered analysis. Historical startup data and ML-based similarity analysis are planned for future development.

## Features

- **AI-Powered Startup Analysis:** Generate structured analysis reports for submitted startup ideas.
- **Failure Risk Assessment:** Explore potential business, financial, operational, and market-related risks.
- **Competitor Analysis:** Identify relevant competitors and competitive challenges for further evaluation.
- **Actionable Recommendations:** Receive practical suggestions and strategies to improve startup viability.
- **Analysis History:** Access previously generated startup analysis reports.
- **User Authentication:** User account functionality for accessing the platform.
- **Persistent Storage:** Store startup analysis sessions and reports for future access.

## Tech Stack

| Component | Technology |
|---|---|
| Frontend | HTML, CSS, JavaScript |
| Backend | Java, Spring Boot |
| Database | MySQL |
| AI Integration | Groq API |
| AI Model | Configurable Groq model |
| Build Tool | Maven |
| IDE | Spring Tool Suite (STS) |
| Version Control | Git & GitHub |

## System Architecture

```text
User
  |
  v
Frontend
HTML / CSS / JavaScript
  |
  v
Spring Boot Backend
  |
  v
Startup Analysis Service
  |
  v
Analysis Provider
  |
  v
AI Service
  |
  v
Groq API
  |
  v
Generated Analysis Report
  |
  v
MySQL Database
  |
  v
Analysis History
```

## How It Works

1. The user submits a startup idea through the frontend.
2. The backend validates and processes the analysis request.
3. The analysis provider prepares the prompt.
4. The AI service sends the request to the Groq API.
5. The generated analysis report is returned to the application.
6. The analysis session and report are persisted.
7. The user can revisit previous analyses through analysis history.

## Getting Started

### Prerequisites

- Java JDK compatible with the project's Spring Boot configuration
- Maven (or Maven Wrapper, if included)
- MySQL
- Node.js is not required for a static frontend unless additional tooling is configured
- A Groq API key

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/GorangGiri09/SFIP.git
cd SFIP
```

**2. Configure the database**

Create a MySQL database and configure the database connection according to your Spring Boot configuration.

**3. Configure environment variables**

Set the following environment variables in your development environment:

```text
GROQ_API_KEY=your_groq_api_key
GROQ_MODEL=openai/gpt-oss-20b
```

Configure your database credentials and other required application secrets separately.

The backend should reference these variables through its configuration.

**4. Run the backend**

From the Spring Boot project directory:

```bash
mvn spring-boot:run
```

Alternatively, run the main Spring Boot application class from your IDE.

**5. Open the frontend**

Open the configured frontend entry page using your development setup.

If the frontend is served through Spring Boot, use the application's configured local URL.

## Project Status

**Current Development Stage:** AI-powered startup analysis platform

### Implemented
- Frontend and backend integration
- Groq API integration
- Startup idea analysis workflow
- Analysis session and history functionality

### Planned
- Historical startup failure dataset integration
- ML-based similarity analysis
- Improved evidence-backed startup risk assessment
- Further evaluation of analysis accuracy and reliability

## Limitations

- AI-generated analysis may contain inaccurate or incomplete information.
- Competitor information and startup failure claims should be independently verified.
- Risk assessments are informational and do not guarantee startup success or failure.
- ML-based historical similarity analysis is planned and is not currently represented as a completed feature.

## Security

- Never commit API keys, database passwords, JWT secrets, or other credentials.
- Use environment variables or an appropriate secrets management solution.
- Keep local configuration and generated build files out of version control.

## Future Vision

SFIP aims to combine AI-assisted startup analysis with historical startup failure data and ML-based similarity techniques to provide more data-informed insights into startup risks and business viability.


---

*SFIP — Learn from startup failures. Build smarter ventures.*
