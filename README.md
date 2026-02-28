# Borod Bank — CI Repository

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Docker](https://img.shields.io/badge/docker-vlad7774%2Fborod--bank-blue)](https://hub.docker.com/r/vlad7774/borod-bank)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Multi-tier banking web application built with **Spring Boot 3.4.5** and **Spring Security 6**, deployed via a **20+ stage Jenkins CI pipeline** with a dedicated **12-stage nightly regression suite**.

---

## CI Pipeline Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                    JENKINS CI PIPELINE (20+ stages)                  │
│                                                                      │
│  Checkout                                                            │
│    ├── [Parallel] Gitleaks ── Hadolint ── Trivy FS ── Integration   │
│    │                                                    Tests        │
│    ▼                                                                 │
│  Compile                                                             │
│    ├── [Parallel] Unit Tests ── SonarQube Analysis                   │
│    ▼                                                                 │
│  Quality Gate (webhook callback, 5 min timeout)                      │
│    ▼                                                                 │
│  Build JAR → Docker Build & Push                                     │
│    ├── [Parallel] Trivy Image ── Checkov IaC ── SBOM (Syft)         │
│    ▼                                                                 │
│  Cosign Sign & Attest                                                │
│    ▼                                                                 │
│  Update K8s Manifests (CD repo)                                      │
│    ▼                                                                 │
│  Smoke Test + E2E Login (register → login → /dashboard)             │
│    ▼                                                                 │
│  Slack Notification                                                  │
└──────────────────────────────────────────────────────────────────────┘
```

## Nightly Regression Pipeline (14 stages, daily at 2 AM)

```
┌──────────────────────────────────────────────────────────────────────┐
│                   NIGHTLY PIPELINE (14 stages)                       │
│                                                                      │
│  Checkout                                                            │
│    ▼ Unit Tests + Coverage (JaCoCo, 70% threshold)                  │
│    ▼ Integration Tests (MySQL sidecar)                               │
│    ▼ Architecture Tests (ArchUnit, 6 rules)                          │
│    ▼ Mutation Testing (PIT, 60% threshold)                           │
│    ▼ License Compliance (GPL/AGPL check)                             │
│    ▼ Performance Test (k6: 10 VUs, 30s)                              │
│    ▼ AI Nightly Analysis (Claude API Haiku)                          │
│    ▼ Smoke Tests + E2E Login                                         │
│    ▼ DAST Scan (OWASP ZAP)                                          │
│    ▼ Pen Testing (Nuclei)                                            │
│    ▼ Push 17 Metrics → Pushgateway → Prometheus → Grafana            │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.4.5, Spring Security 6.4 |
| Database | MySQL 8 (AWS RDS in production) |
| Frontend | Thymeleaf + Bootstrap 4 |
| Build | Maven 3.9 |
| Container | Docker (Eclipse Temurin 17 Alpine) |
| CI | Jenkins Pipeline (ephemeral Kubernetes pod agents) |
| Security Scanning | Gitleaks, Hadolint, Trivy, SonarQube, Checkov, OWASP ZAP |
| Supply Chain | Syft (SBOM), Cosign (image signing) |
| Performance | k6 load testing |
| Mutation Testing | PIT (pitest) |
| Architecture Testing | ArchUnit |

---

## Security Scanning (6 tools)

| Tool | What it Scans | Stage |
|---|---|---|
| **Gitleaks** | Secrets in source code | Pre-build (parallel) |
| **Hadolint** | Dockerfile best practices | Pre-build (parallel) |
| **Trivy (FS)** | Filesystem vulnerabilities | Pre-build (parallel) |
| **SonarQube** | Code quality + security hotspots | Post-compile |
| **Trivy (Image)** | Container image CVEs | Post-build (parallel) |
| **Checkov** | IaC misconfigurations | Post-build (parallel) |

Additionally in nightly: **OWASP ZAP** (DAST), **Nuclei** (pen testing), and **License Compliance** scanning.

---

## Testing

| Type | Tool | Threshold | Pipeline |
|---|---|---|---|
| Unit Tests | JUnit 5 | Must pass | CI + Nightly |
| Integration Tests | Spring Boot Test + MySQL | Must pass | CI + Nightly |
| E2E Login | Custom (register → login → dashboard) | Must pass | CI + Nightly |
| Architecture Tests | ArchUnit (6 rules) | Must pass | Nightly |
| Mutation Testing | PIT | 60% mutation score | Nightly |
| Code Coverage | JaCoCo | 70% line coverage | Nightly |
| Performance | k6 (10 VUs, 30s) | P95 < 2000 ms | Nightly |
| DAST | OWASP ZAP | Informational | Nightly |
| Pen Testing | Nuclei | Informational | Nightly |

---

## Project Structure

```
borod-bank-ci/
├── Dockerfile                          # Multi-stage container build
├── Jenkinsfile                         # Main CI pipeline (20+ stages)
├── Jenkinsfile.nightly                 # Nightly regression (14 stages)
├── pom.xml                             # Maven config (Spring Boot 3.4.5)
├── docker-compose.yml                  # Local development with MySQL
├── zap-config.conf                     # OWASP ZAP scan configuration
├── dependency-check-suppressions.xml   # CVE suppressions
├── scripts/
│   └── aiops_analyzer.py              # AI code review script
├── src/
│   ├── main/
│   │   ├── java/com/vlad774/borodbank/
│   │   │   ├── BorodBankApplication.java
│   │   │   ├── config/SecurityConfig.java
│   │   │   ├── controller/BankController.java
│   │   │   ├── model/{Account,Transaction}.java
│   │   │   ├── repository/{Account,Transaction}Repository.java
│   │   │   └── service/{AccountService,FeatureFlagService}.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── static/sql/init.sql
│   │       ├── static/css/
│   │       └── templates/{login,register,dashboard,transactions}.html
│   └── test/
│       └── java/com/vlad774/borodbank/
│           ├── BorodBankApplicationTests.java
│           ├── AccountServiceIT.java
│           ├── integration/TransferAtomicityIT.java
│           ├── unit/controller/BankControllerTest.java
│           ├── unit/service/{AccountServiceTest,FeatureFlagServiceTest}.java
│           └── architecture/ArchitectureTest.java
└── .github/workflows/                  # GitHub Actions for PR checks
```

---

## Quick Start (Local Development)

```bash
# Prerequisites: Java 17+, Docker

# Option 1: docker-compose
docker-compose up -d

# Option 2: manual
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS borodbankdb;"
./mvnw spring-boot:run

# Access at http://localhost:8080
```

## Environment Variables

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL (`jdbc:mysql://host:3306/borodbankdb`) |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |

In production, credentials are managed by **External Secrets Operator** syncing from **AWS Secrets Manager**.

---

## Related Repositories

| Repository | Description |
|---|---|
| [borod-bank-cd](https://github.com/Synaptix-Innovations/borod-bank-cd) | Kubernetes manifests + ArgoCD App of Apps |
| [borod-bank-infra](https://github.com/Synaptix-Innovations/borod-bank-infra) | Terraform IaC for AWS EKS |
| [jenkins-setup](https://github.com/Synaptix-Innovations/jenkins-setup) | Custom Jenkins controller image |
| [borod-bank-docs](https://github.com/Synaptix-Innovations/borod-bank-docs) | Project documentation and screenshots |

## Author

**Vlad Vesninsky** — [GitHub](https://github.com/Vlad774)
