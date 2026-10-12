# Centralized SLCM Portal

## Introduction

The **Centralized SLCM Portal** is a web-based application designed to bring student academic information and administrative operations into a single, centralized platform. It aims to simplify access to academic records, organize student-related information, and provide role-based access for students and administrators.

The project follows a full-stack architecture, with a React-based frontend, a Java Spring Boot backend, and a MySQL database. The frontend communicates with the backend through REST APIs, while the backend handles business logic, authentication, and database operations.

## Objectives

- Centralize student and academic information in one portal.
- Provide separate access for students and administrators.
- Implement secure login and role-based authorization.
- Organize academic data through a structured MySQL database.
- Provide a responsive and user-friendly interface.
- Build a maintainable application using modern full-stack technologies.

## Technology Stack

| Layer | Technologies |
|---|---|
| Frontend | React.js, JavaScript, HTML, CSS |
| Backend | Java, Spring Boot |
| API | REST APIs |
| Database | MySQL |
| Database access | JDBC |
| Authentication | Spring Security, JWT |
| Build tools | Maven, npm |
| Development tools | Visual Studio Code, MySQL Workbench |

## Architecture

1. **Frontend:** Provides the user interface for students and administrators.
2. **Backend:** Processes requests, enforces application rules, and manages authentication.
3. **Database:** Stores user accounts and academic information in MySQL.
4. **REST APIs:** Connect the React frontend with the Spring Boot backend.

## Project Structure

```text
MySLCM_project/
├── backend/       # Spring Boot application
├── frontend/      # React application
├── database/      # Database scripts
├── docs/          # Project documentation
├── .env.example   # Environment configuration template
├── .gitignore
└── README.md
```

## Project Status

The project is under development. Features and implementation details may evolve as the application progress.

