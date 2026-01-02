# Task Management Application

A RESTful API built with Spring Boot for managing tasks with user authentication and authorization using JWT tokens.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Running with Docker](#running-with-docker)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Configuration](#configuration)

## 🎯 Overview

This Task Management Application provides a secure backend API for creating, reading, updating, and deleting tasks. It includes user registration and authentication using JWT (JSON Web Tokens) and integrates with a PostgreSQL database.

## ✨ Features

- **User Management**
  - User registration with password encryption
  - JWT-based authentication
  - Secure password storage using BCrypt

- **Task Management**
  - Create new tasks with title, description, and status
  - Retrieve individual tasks or all tasks
  - Update existing tasks
  - Delete tasks
  - Task status validation (To Do, In Progress, Done)
  - Duplicate title prevention

- **Security**
  - JWT token-based authentication
  - Spring Security integration
  - CORS configuration for frontend integration
  - Protected endpoints with Bearer authentication

- **API Features**
  - RESTful API design
  - Input validation
  - Comprehensive error handling
  - Swagger/OpenAPI documentation
  - Actuator endpoints for monitoring

## 🛠 Technology Stack

- **Framework:** Spring Boot 3.5.9
- **Language:** Java 21
- **Database:** PostgreSQL
- **Security:** Spring Security with JWT
- **ORM:** Spring Data JPA with Hibernate
- **API Documentation:** SpringDoc OpenAPI (Swagger)
- **Build Tool:** Maven
- **Testing:** JUnit 5, Mockito
- **Code Coverage:** JaCoCo (80% minimum)
- **Containerization:** Docker

### Key Dependencies

- `spring-boot-starter-web` - RESTful web services
- `spring-boot-starter-data-jpa` - Database operations
- `spring-boot-starter-security` - Security framework
- `spring-boot-starter-validation` - Input validation
- `spring-boot-starter-actuator` - Application monitoring
- `jjwt` (v0.13.0) - JWT token generation and validation
- `postgresql` - PostgreSQL JDBC driver
- `lombok` - Reduce boilerplate code
- `springdoc-openapi` (v2.8.14) - API documentation

## 📦 Prerequisites

- Java 21 or higher
- Maven 3.6+
- PostgreSQL 12+
- Docker (optional, for containerized deployment)

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd TaskManagementApplication
```

### 2. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE taskdb;
```

### 3. Configure Application

Update `src/main/resources/application.yaml` with your database credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/taskdb
    username: postgres
    password: your_password
```

### 4. Build the Application

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR file:

```bash
java -jar target/tasks-management-application-1.0-RELEASE.jar
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

### Swagger UI

Access the interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

### Actuator Endpoints

Monitor the application health and metrics:
- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`

### Authentication Endpoints

#### Register New User
```http
POST /user/new-registration
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "firstname": "John",
  "lastname": "Doe",
  "country": "USA",
  "roles": "ROLE_USER"
}
```

#### Login
```http
POST /user/authenticate
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

Response includes JWT token in the `Authorization` header.

### Task Management Endpoints

All task endpoints require Bearer token authentication.

#### Create Task
```http
POST /v1/saveTask
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Complete Project",
  "description": "Finish the Spring Boot project",
  "status": "To Do"
}
```

#### Get Task by ID
```http
GET /v1/getTask/{taskId}
Authorization: Bearer <token>
```

#### Get All Tasks
```http
GET /v1/getAllTasks
Authorization: Bearer <token>
```

#### Update Task
```http
PUT /v1/updateTask
Authorization: Bearer <token>
Content-Type: application/json

{
  "id": 1,
  "title": "Complete Project",
  "description": "Finish and deploy the Spring Boot project",
  "status": "In Progress"
}
```

#### Delete Task
```http
DELETE /v1/deleteTask/{id}
Authorization: Bearer <token>
```

### Status Values

Valid task statuses:
- `To Do`
- `In Progress`
- `Done`

## 🐳 Running with Docker

### Build the Application

```bash
mvn clean package -DskipTests
```

### Build Docker Image

```bash
docker build -t task-management-app:1.0 .
```

### Run with Docker Compose

Ensure PostgreSQL is running on your host machine, then:

```bash
docker-compose up -d
```

The application will be available at `http://localhost:8080`

To stop:

```bash
docker-compose down
```

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run Tests with Coverage

```bash
mvn clean test jacoco:report
```

View the coverage report at:
```
target/site/jacoco/index.html
```

### Code Coverage Requirements

The project enforces a minimum of 80% line coverage using JaCoCo.

### Test Classes

- `TaskManagementControllerTest` - Controller layer tests
- `UserServicesControllerTest` - User authentication tests
- `TaskServiceImplTest` - Task service layer tests
- `UserDetailsServiceImplTest` - User details service tests
- `RegistrationServiceTest` - Registration service tests
- `JWTServiceTest` - JWT service tests
- `JWTFilterTest` - JWT filter tests

## 📁 Project Structure

```
TaskManagementApplication/
├── src/
│   ├── main/
│   │   ├── java/com/restapp/
│   │   │   ├── config/          # Security and Swagger configuration
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dao/             # Repository interfaces
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Custom exceptions
│   │   │   ├── filter/          # JWT authentication filter
│   │   │   ├── service/         # Business logic services
│   │   │   ├── validation/      # Custom validators
│   │   │   └── TaskManagementApplication.java
│   │   └── resources/
│   │       └── application.yaml # Application configuration
│   └── test/                    # Unit and integration tests
├── target/                      # Build output
├── docker-compose.yml           # Docker Compose configuration
├── Dockerfile                   # Docker image definition
├── pom.xml                      # Maven dependencies
└── README.md                    # This file
```

## ⚙️ Configuration

### Application Properties

Key configuration in `application.yaml`:

```yaml
spring:
  application:
    name: taskmanager-app
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/taskdb
    username: postgres
    password: XXXXXXXX
  jpa:
    hibernate:
      ddl-auto: update
```

### CORS Configuration

The application is configured to accept requests from `http://localhost:5173` (React frontend).

To modify CORS settings, update the `@CrossOrigin` annotation in controllers.

### JWT Configuration

JWT tokens expire after 30 minutes. To modify the expiration time, update the `JWTService` class.

## 🔒 Security

- Passwords are encrypted using BCrypt
- JWT tokens are required for all task management endpoints
- User registration endpoint is public
- Authentication endpoint is public
- All other endpoints are protected

## 📝 License

This project is available for educational and development purposes.

## 👥 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📧 Support

For issues and questions, please open an issue in the repository.

---

**Version:** 1.0-RELEASE  
**Last Updated:** January 2026

