# SFIP — Startup Failure Intelligence Platform

An AI-powered platform designed to analyze startup ideas, identify potential failure risks, and provide actionable insights to help entrepreneurs make more informed decisions.

SFIP combines AI-powered analysis with historical startup data and machine learning-based similarity analysis to help founders understand potential challenges before building their ventures.

---

## About the Project

The **Startup Failure Intelligence Platform (SFIP)** helps entrepreneurs evaluate startup ideas through structured AI-assisted analysis and historical startup intelligence.

The platform analyzes different aspects of a startup idea, including:

- Market challenges and business viability
- Financial and funding-related risks
- Operational and execution challenges
- Competitive threats
- Potential failure patterns
- Actionable recommendations

SFIP integrates the Groq API for AI-powered analysis and a Python-based machine learning service for startup prediction and historical similarity retrieval.

The goal is to provide founders with a more structured and data-informed perspective when evaluating their startup ideas.

## Features

- **AI-Powered Startup Analysis:** Generate structured analysis reports using the Groq API.
- **Startup Feature Extraction:** Extract relevant startup attributes from user-submitted descriptions.
- **ML-Based Startup Assessment:** Evaluate how closely a startup resembles historically failed or successful startups.
- **Similar Startup Retrieval:** Find historically failed and successful startups with similar characteristics.
- **Failure Risk Assessment:** Explore business, financial, operational, and market-related risks.
- **Actionable Recommendations:** Receive practical suggestions to improve startup viability.
- **Analysis History:** Access previously generated startup analysis reports.
- **User Authentication:** Secure user account functionality using JWT-based authentication.
- **Persistent Storage:** Store startup analysis sessions and reports in MySQL.
- **Structured Reports:** Render AI-generated Markdown reports with formatted headings, lists, tables, and other elements.

---

## Tech Stack

| Component | Technology |
|---|---|
| Frontend | HTML, CSS, JavaScript |
| Backend | Java, Spring Boot |
| Machine Learning Service | Python, FastAPI |
| Database | MySQL |
| AI Integration | Groq API |
| AI Model | Configurable Groq model |
| ML Framework | Scikit-learn |
| Similarity Search | TF-IDF and Cosine Similarity |
| Authentication | JWT |
| ORM / Data Access | Spring Data JPA, Hibernate |
| Build Tool | Maven |
| IDE | Spring Tool Suite (STS) |
| Version Control | Git and GitHub |

---

## System Architecture

```text
                   USER
                     |
                     v
              Frontend Application
              HTML / CSS / JavaScript
                     |
                     v
              Spring Boot Backend
                  Port 8080
                     |
                     v
             Startup Analysis Service
                     |
          +----------+-----------+
          |                      |
          v                      v
    Groq AI Service          ML Service Client
          |                      |
          v                      v
       Groq API              FastAPI Server
                              Port 8000
                                 |
                                 v
                         Startup ML Model
                                 |
                       +---------+---------+
                       |                   |
                       v                   v
                  Prediction         Similarity Search
                       |                   |
                       +---------+---------+
                                 |
                                 v
                           JSON Response
                                 |
                                 v
                        Spring Boot Backend
                                 |
                                 v
                           MySQL Database
                                 |
                                 v
                         Analysis History
                                 |
                                 v
                         Frontend Application
```

## How It Works

1. The user submits a startup idea through the frontend.
2. The Spring Boot backend receives and validates the analysis request.
3. The startup analysis service coordinates the analysis workflow.
4. The Groq AI service extracts structured startup features from the submitted idea.
5. The backend sends the extracted features to the Python FastAPI ML service.
6. The ML service evaluates the startup using its trained model and retrieves similar failed and successful startups.
7. The backend uses the prediction and similarity results to generate a structured AI analysis report through Groq.
8. The generated report and analysis session are persisted in MySQL.
9. The frontend displays the formatted report.
10. Users can revisit their previously generated analysis reports through analysis history.

---

# Getting Started

## Prerequisites

Install the following before running the project:

- Java JDK 21
- Maven
- MySQL 8.0 or compatible version
- Python 3.10 or compatible version supported by the ML dependencies
- pip
- Git
- A Groq API key

## 1. Clone the Repository

```bash
git clone https://github.com/GorangGiri09/SFIP.git
cd SFIP
```

## 2. Configure MySQL

Start your MySQL server and create the database:

```sql
CREATE DATABASE sfip_db;
```

The application uses the following database configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sfip_db?serverTimezone=Asia/Kolkata
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Make sure your MySQL server is running and the configured database user has the required permissions.

Hibernate is configured to update the database schema through the application's JPA configuration.

---

## 3. Configure Environment Variables

SFIP uses environment variables to keep API keys, database credentials, JWT secrets, and service configuration outside the source code.

The backend reads these variables through:

`src/main/resources/application.properties`

### Required Environment Variables

| Variable | Purpose |
|---|---|
| `DB_USERNAME` | MySQL database username |
| `DB_PASSWORD` | MySQL database password |
| `JWT_SECRET` | Secret key used for JWT signing |
| `GROQ_API_KEY` | API key for Groq AI integration |
| `GROQ_MODEL` | Groq model identifier |
| `ML_SERVICE_BASE_URL` | Base URL of the FastAPI ML service |

### Optional Environment Variables

| Variable | Default / Purpose |
|---|---|
| `GEMINI_API_KEY` | Gemini API key, if the corresponding provider is used |
| `GEMINI_MODEL` | Gemini model configuration |
| `ML_SERVICE_BASE_URL` | Defaults to `http://localhost:8000` when not provided |
| `GROQ_MODEL` | Defaults to `openai/gpt-oss-20b` when not provided |

### Example Environment Configuration

Set the following variables in your development environment. Replace the example values with your own credentials.

```text
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password

JWT_SECRET=your_long_random_secret_key

GROQ_API_KEY=your_groq_api_key
GROQ_MODEL=openai/gpt-oss-20b

ML_SERVICE_BASE_URL=http://localhost:8000

GEMINI_API_KEY=your_gemini_api_key
GEMINI_MODEL=your_gemini_model
```

**Important Security Notes:**

- Never commit actual API keys or passwords to GitHub.
- Do not hardcode secrets inside `application.properties`.
- Do not commit `.env` files containing actual credentials.
- Use environment variables or a secure secrets manager.
- Use a sufficiently long, randomly generated JWT secret.
- Keep local development credentials separate from production credentials.

### Setting Environment Variables on Windows PowerShell

For the current terminal session:

```powershell
$env:DB_USERNAME="your_mysql_username"
$env:DB_PASSWORD="your_mysql_password"

$env:JWT_SECRET="your_long_random_secret_key"

$env:GROQ_API_KEY="your_groq_api_key"
$env:GROQ_MODEL="openai/gpt-oss-20b"

$env:ML_SERVICE_BASE_URL="http://localhost:8000"
```

Optional Gemini configuration:

```powershell
$env:GEMINI_API_KEY="your_gemini_api_key"
$env:GEMINI_MODEL="your_gemini_model"
```

These variables remain available only in the current PowerShell session and its child processes.

For permanent Windows environment configuration, use Windows Environment Variables settings or PowerShell's `[Environment]::SetEnvironmentVariable()` method.

---

# 4. Run the Python ML Service

SFIP uses a separate Python FastAPI service for startup prediction and similar startup retrieval.

The ML service should be started independently from the Spring Boot backend.

### Step 1: Navigate to the ML project directory

```bash
cd path/to/startup-success-model
```

Use the actual location of your ML project.

### Step 2: Create a virtual environment

Windows:

```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
```

Linux / macOS:

```bash
python -m venv .venv
source .venv/bin/activate
```

### Step 3: Install dependencies

```bash
pip install -r requirements.txt
```

Ensure the requirements include FastAPI and Uvicorn if they are not already present:

```bash
pip install fastapi uvicorn
```

### Step 4: Start the FastAPI server

From the ML service root directory:

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Step 5: Verify the ML service

Open the following URLs:

| Purpose | URL |
|---|---|
| ML Service | `http://localhost:8000` |
| Health Check | `http://localhost:8000/health` |
| Swagger Documentation | `http://localhost:8000/docs` |
| ReDoc Documentation | `http://localhost:8000/redoc` |

The FastAPI service exposes the following endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/health` | Check ML service availability |
| POST | `/predict` | Startup success similarity prediction |
| POST | `/similar-startups` | Retrieve similar failed and successful startups |

Keep the FastAPI server running while using the Spring Boot backend.

For further information about the ML model, input/output contracts, evaluation metrics, and limitations, refer to the ML service repository's README.

---

# 5. Run the Spring Boot Backend

Once MySQL and the FastAPI ML service are running, start the Spring Boot application.

Navigate to the backend project root directory containing `pom.xml`.

### Using Maven

```bash
mvn spring-boot:run
```

### Using Maven Wrapper

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```bash
./mvnw spring-boot:run
```

Alternatively, run the main Spring Boot application class through Spring Tool Suite (STS).

The backend runs on:

```text
http://localhost:8080
```

Make sure the environment variables are available in the terminal or IDE process that starts Spring Boot.

---

# 6. Access the Frontend

The frontend is served through the Spring Boot application's static resources.

After starting the backend, open:

```text
http://localhost:8080/dashboard.html
```

The frontend uses the Spring Boot backend for authentication, startup analysis, and analysis history.

No separate Node.js development server is required for the current static HTML, CSS, and JavaScript frontend.

---

# 7. Local Development Architecture

The following services must be running for the complete analysis workflow:

| Component | Local Address | Purpose |
|---|---|---|
| Spring Boot Backend | `http://localhost:8080` | Main application API |
| FastAPI ML Service | `http://localhost:8000` | Startup prediction and similarity retrieval |
| MySQL | `localhost:3306` | Persistent data storage |
| Frontend | `http://localhost:8080/dashboard.html` | User interface |

### Recommended Startup Order

1. Start MySQL.
2. Start the Python FastAPI ML service.
3. Start the Spring Boot backend.
4. Open the frontend in your browser.

---

# Project Status

**Current Development Stage:** AI-powered startup failure intelligence platform with integrated machine learning services.

### Implemented

- Frontend and Spring Boot backend integration.
- User authentication functionality.
- Groq API integration.
- Startup idea analysis workflow.
- AI-assisted startup feature extraction.
- Python FastAPI ML service integration.
- Startup prediction using historical startup data.
- Similar failed and successful startup retrieval.
- Analysis session persistence using MySQL.
- Analysis history functionality.
- Structured Markdown report rendering.

### Future Improvements

- Expand historical startup datasets.
- Improve the representativeness of training data.
- Add more evidence-backed startup risk indicators.
- Improve model evaluation and reliability.
- Enhance the analysis experience and report generation.
- Explore further deployment and scalability improvements.

---

# Limitations

- AI-generated analysis may contain inaccurate or incomplete information.
- Competitor information and startup failure claims should be independently verified.
- The ML success score is a comparative model score, not the actual probability of startup success.
- The model is trained on notable startups and may not fully represent ordinary early-stage businesses.
- Predictions can be less reliable when input features are missing or outside the training data range.
- The platform provides decision-support insights and does not guarantee startup success or failure.

---

# Security

- Never commit API keys, database passwords, or JWT secrets.
- Keep `.env` files and other local credential files out of version control.
- Use environment variables for sensitive configuration.
- Do not expose development servers directly to the public internet.
- Configure production secrets and service URLs separately from local development settings.
- Use HTTPS and appropriate access controls when deploying services.

---

# Future Vision

SFIP aims to help entrepreneurs learn from historical startup outcomes by combining AI-powered reasoning, structured startup feature extraction, machine learning-based prediction, and similarity retrieval.

The long-term vision is to create a comprehensive startup intelligence platform that helps founders identify potential weaknesses, understand historical failure patterns, and make more informed business decisions.

---

*SFIP — Learn from startup failures. Build smarter ventures.*
