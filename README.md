# Borod Bank - CI Repository

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Docker](https://img.shields.io/badge/docker-vlad7774%2Fborod--bank-blue)](https://hub.docker.com/r/vlad7774/borod-bank)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Multi-tier banking web application built with Spring Boot, secured with Spring Security, and deployed via Jenkins CI/CD pipeline.

```
┌──────────────────────────────────────────────────────────────┐
│                     CI PIPELINE FLOW                         │
│                                                              │
│  Git Checkout ──► Compile ──► Tests ──► SonarQube            │
│       │                                    │                 │
│       │              ┌─────────────────────┘                 │
│       │              ▼                                       │
│       │         Trivy FS Scan ──► Build JAR                  │
│       │                              │                       │
│       │              ┌───────────────┘                       │
│       │              ▼                                       │
│       │         Docker Build ──► Trivy Image Scan            │
│       │                              │                       │
│       │              ┌───────────────┘                       │
│       │              ▼                                       │
│       └────── Docker Push ──► Update K8s Manifests (CD repo) │
└──────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer      | Technology                     |
|------------|--------------------------------|
| Backend    | Java 17, Spring Boot 3.3       |
| Security   | Spring Security, BCrypt        |
| Database   | MySQL 8                        |
| Templates  | Thymeleaf + Bootstrap 4        |
| Build      | Maven 3.9                      |
| Container  | Docker (Eclipse Temurin Alpine) |
| CI         | Jenkins Pipeline               |
| Scanning   | SonarQube, Trivy               |

## Project Structure

```
borod-bank-ci/
├── Dockerfile
├── Jenkinsfile
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/vlad774/borodbank/
    │   │   ├── BorodBankApplication.java
    │   │   ├── config/SecurityConfig.java
    │   │   ├── controller/BankController.java
    │   │   ├── model/{Account,Transaction}.java
    │   │   ├── repository/{Account,Transaction}Repository.java
    │   │   └── service/AccountService.java
    │   └── resources/
    │       ├── application.properties
    │       ├── static/sql/init.sql
    │       └── templates/{login,register,dashboard,transactions}.html
    └── test/
        └── java/com/vlad774/borodbank/
            └── BorodBankApplicationTests.java
```

## Quick Start (Local Development)

```bash
# Prerequisites: Java 17+, MySQL 8 running on localhost:3306

# Create the database
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS borodbankdb;"

# Run the application
./mvnw spring-boot:run

# Access at http://localhost:8080
```

## Environment Variables

| Variable                     | Default                                              | Description        |
|------------------------------|------------------------------------------------------|--------------------|
| `SPRING_DATASOURCE_URL`     | `jdbc:mysql://localhost:3306/borodbankdb?...`         | Database JDBC URL  |
| `SPRING_DATASOURCE_USERNAME`| `root`                                               | Database username  |
| `SPRING_DATASOURCE_PASSWORD`| `Test@123`                                           | Database password  |

## Docker

```bash
# Build
docker build -t vlad7774/borod-bank:latest .

# Run (requires MySQL accessible at mysql-service:3306)
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-service:3306/borodbankdb \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  vlad7774/borod-bank:latest
```

## Related Repositories

| Repository | Description |
|------------|-------------|
| [borod-bank-cd](https://github.com/Vlad774/borod-bank-cd) | Kubernetes manifests + ArgoCD |
| [borod-bank-infra](https://github.com/Vlad774/borod-bank-infra) | Terraform for AWS EKS |

## Author

**Vlad Vesninsky** - [GitHub](https://github.com/Vlad774)
