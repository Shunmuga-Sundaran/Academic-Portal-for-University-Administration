-- ============================================================
--  Academic Portal for University Administration - MySQL schema
--  Normalized to 3NF. Run once:  mysql -u root -p < sql/schema.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS academic_portal CHARACTER SET utf8mb4;
USE academic_portal;

-- Lookup tables --------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    role_id   INT PRIMARY KEY,
    role_name VARCHAR(20) NOT NULL UNIQUE
);
INSERT IGNORE INTO roles (role_id, role_name) VALUES (1, 'ADMIN'), (2, 'FACULTY'), (3, 'STUDENT');

CREATE TABLE IF NOT EXISTS departments (
    dept_id   INT AUTO_INCREMENT PRIMARY KEY,
    dept_name VARCHAR(80) NOT NULL UNIQUE
);
INSERT IGNORE INTO departments (dept_name) VALUES
    ('Computer Science'), ('Electronics'), ('Mechanical'), ('Civil');

-- Authentication (RBAC: every user has exactly one role) ----------
CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role_id       INT          NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (role_id)
);

-- Profiles ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NOT NULL UNIQUE,
    roll_no    VARCHAR(20)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    phone      CHAR(10)     NOT NULL,
    dept_id    INT          NOT NULL,
    semester   TINYINT      NOT NULL CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_students_dept FOREIGN KEY (dept_id) REFERENCES departments (dept_id)
);

CREATE TABLE IF NOT EXISTS faculty (
    faculty_id  INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    dept_id     INT          NOT NULL,
    designation VARCHAR(50)  NOT NULL,
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_faculty_dept FOREIGN KEY (dept_id) REFERENCES departments (dept_id)
);

-- Courses & enrollments --------------------------------------------
CREATE TABLE IF NOT EXISTS courses (
    course_id   INT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(15)  NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    credits     TINYINT      NOT NULL CHECK (credits BETWEEN 1 AND 6),
    dept_id     INT          NOT NULL,
    faculty_id  INT          NULL,
    CONSTRAINT fk_courses_dept    FOREIGN KEY (dept_id)    REFERENCES departments (dept_id),
    CONSTRAINT fk_courses_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id    INT       NOT NULL,
    course_id     INT       NOT NULL,
    enrolled_on   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_enrollment UNIQUE (student_id, course_id),
    CONSTRAINT fk_enr_student FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE,
    CONSTRAINT fk_enr_course  FOREIGN KEY (course_id)  REFERENCES courses (course_id)   ON DELETE CASCADE
);

-- Examination -------------------------------------------------------
CREATE TABLE IF NOT EXISTS exams (
    exam_id    INT AUTO_INCREMENT PRIMARY KEY,
    course_id  INT         NOT NULL,
    exam_name  VARCHAR(50) NOT NULL,
    exam_date  DATE        NOT NULL,
    max_marks  INT         NOT NULL CHECK (max_marks > 0),
    CONSTRAINT uq_exam UNIQUE (course_id, exam_name),
    CONSTRAINT fk_exams_course FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS results (
    result_id      INT AUTO_INCREMENT PRIMARY KEY,
    exam_id        INT           NOT NULL,
    student_id     INT           NOT NULL,
    marks_obtained DECIMAL(5,2)  NOT NULL CHECK (marks_obtained >= 0),
    CONSTRAINT uq_result UNIQUE (exam_id, student_id),
    CONSTRAINT fk_res_exam    FOREIGN KEY (exam_id)    REFERENCES exams (exam_id)       ON DELETE CASCADE,
    CONSTRAINT fk_res_student FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE
);

-- Fee management ----------------------------------------------------
CREATE TABLE IF NOT EXISTS fees (
    fee_id      INT AUTO_INCREMENT PRIMARY KEY,
    student_id  INT            NOT NULL,
    description VARCHAR(100)   NOT NULL,
    amount      DECIMAL(10,2)  NOT NULL CHECK (amount > 0),
    due_date    DATE           NOT NULL,
    CONSTRAINT fk_fees_student FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id   INT AUTO_INCREMENT PRIMARY KEY,
    fee_id       INT            NOT NULL,
    amount_paid  DECIMAL(10,2)  NOT NULL CHECK (amount_paid > 0),
    payment_mode VARCHAR(20)    NOT NULL,
    paid_on      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_fee FOREIGN KEY (fee_id) REFERENCES fees (fee_id) ON DELETE CASCADE
);
