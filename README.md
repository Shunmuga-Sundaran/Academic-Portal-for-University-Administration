# Academic Portal for University Administration

A full-stack web application that streamlines **student, faculty, course, examination and fee management** for a
university. Built with **Java, Object-Oriented Programming, MySQL and JDBC** on the backend and **HTML, CSS and
JavaScript** on the frontend, with a normalized database schema, efficient CRUD operations and
**Role-Based Access Control (RBAC)** for administrators, faculty and students.

## Features

| Role | What they can do |
|------|------------------|
| **Admin** | Dashboard; manage departments, students, faculty, courses (full CRUD); enroll / remove students; schedule exams and view results; add fees (one student or all), record payments, see outstanding dues and student ledgers; reset passwords, enable / disable accounts |
| **Faculty** | See only their own courses and class rosters; schedule exams; enter / update marks in a grid; view results with class average and pass count |
| **Student** | Profile; browse, enroll in and drop courses; exam schedule; results with grades and overall percentage; fees, payment history and online payment |

## Tech stack

Java 17+ (built-in `com.sun.net.httpserver`, no Tomcat / Maven needed), MySQL 8, JDBC (MySQL Connector/J),
HTML5, CSS3, vanilla JavaScript (fetch API), Git / GitHub

## Architecture

```
Browser (HTML + CSS + JavaScript)
        |  fetch() -> JSON over HTTP
Router  -> checks session -> checks role (RBAC) -> Controller (Auth / Admin / Faculty / Student)
        -> DAO layer (JDBC, PreparedStatement) -> MySQL
```

- **OOP:** `Controller` interface with four implementations; generic `BaseDAO<T>`; immutable record models;
  custom `ApiException`; separation into model / dao / service / web layers
- **Normalized schema (3NF):** 11 tables, primary and foreign keys, UNIQUE and CHECK constraints
- **RBAC, enforced on the server:**
  1. every API route declares the roles allowed to call it (`403` otherwise)
  2. ownership checks: faculty can only touch their own courses/exams; a student's id always comes from the
     login session, never from the browser
  3. the account is re-checked on every request, so a disabled user is logged out immediately
- **Security:** `PreparedStatement` everywhere (SQL injection), salted PBKDF2 password hashing, random session
  tokens in `HttpOnly` + `SameSite=Strict` cookies, login lockout after 5 failures, output escaping against XSS,
  Content-Security-Policy header, path-traversal protection for static files, password hashes never sent to the browser
- **Data integrity:** transactions (profile + login created together; payments lock the fee row), `BigDecimal`
  for money, fee balance calculated from payments (never stored), cascade rules, a course cannot be dropped once
  marks exist

## Database tables

`roles`, `departments`, `users`, `students`, `faculty`, `courses`, `enrollments`, `exams`, `results`, `fees`, `payments`

## Setup (VS Code, localhost)

1. Install **JDK 17+**, **MySQL 8**, and the VS Code **Extension Pack for Java**.
2. Create the database: `mysql -u root -p < sql/schema.sql`
3. Download `mysql-connector-j-8.4.0.jar`
   (https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar)
   and put it in the `lib/` folder.
4. Copy `db.properties.example` to `db.properties` and enter your MySQL password.
5. Open `src/com/portal/WebApp.java` and click **Run**, then open **http://localhost:8080**.

Command line instead:
```
# Linux / macOS
javac -cp "lib/*" -d out $(find src -name "*.java")
java -cp "out:lib/*" com.portal.WebApp

# Windows PowerShell
javac -cp "lib/*" -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "out;lib/*" com.portal.WebApp
```
Always start it from the project root folder (the one containing `web/` and `src/`).

## First login

Default administrator (created on first run): `admin` / `Admin@123` - change it after logging in.
The admin adds students (username = roll number in lower-case) and faculty, then they can sign in.

## Project structure

```
AcademicPortal/
|-- sql/schema.sql
|-- db.properties.example
|-- lib/                      MySQL Connector/J jar goes here
|-- web/                      frontend
|   |-- index.html (login), admin.html, faculty.html, student.html
|   |-- css/style.css
|   `-- js/ api.js, ui.js, layout.js, login.js, admin.js, faculty.js, student.js
`-- src/com/portal/
    |-- WebApp.java           starts the server on port 8080
    |-- config/               DBConnection
    |-- model/                records: Student, Faculty, Course, Exam, Fee, ...
    |-- dao/                  JDBC data access
    |-- service/              AuthService, AccessControl (RBAC)
    |-- util/                 PasswordUtil, GradeUtil, Validators
    `-- web/                  Router, controllers, sessions, JSON, static files
```

## Possible improvements

Connection pooling (HikariCP), Spring Boot migration, JUnit tests, audit log, CSV / PDF export, attendance module
