package com.portal.dao;

import java.sql.SQLException;

public class EnrollmentDAO {

    public boolean isEnrolled(int studentId, int courseId) throws SQLException {
        return Db.count("SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND course_id = ?",
                studentId, courseId) > 0;
    }

    public void enroll(int studentId, int courseId) throws SQLException {
        Db.update("INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)", studentId, courseId);
    }

    /** A course cannot be dropped once marks exist for it (protects data integrity). */
    public boolean drop(int studentId, int courseId) throws SQLException {
        long marks = Db.count("SELECT COUNT(*) FROM results r JOIN exams e ON r.exam_id = e.exam_id "
                + "WHERE r.student_id = ? AND e.course_id = ?", studentId, courseId);
        if (marks > 0) {
            throw new IllegalStateException("Cannot drop: marks are already recorded for this course.");
        }
        return Db.update("DELETE FROM enrollments WHERE student_id = ? AND course_id = ?",
                studentId, courseId) > 0;
    }
}
