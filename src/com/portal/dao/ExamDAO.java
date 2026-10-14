package com.portal.dao;

import com.portal.model.Exam;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ExamDAO extends BaseDAO<Exam> {

    private static final String SELECT =
            "SELECT e.exam_id, e.course_id, c.course_code, e.exam_name, e.exam_date, e.max_marks "
          + "FROM exams e JOIN courses c ON e.course_id = c.course_id ";

    @Override
    protected Exam map(ResultSet rs) throws SQLException {
        return new Exam(rs.getInt("exam_id"), rs.getInt("course_id"), rs.getString("course_code"),
                rs.getString("exam_name"), rs.getDate("exam_date").toLocalDate(), rs.getInt("max_marks"));
    }

    public void add(int courseId, String name, LocalDate date, int maxMarks) throws SQLException {
        Db.update("INSERT INTO exams (course_id, exam_name, exam_date, max_marks) VALUES (?, ?, ?, ?)",
                courseId, name, date, maxMarks);
    }

    public void delete(int examId) throws SQLException {
        Db.update("DELETE FROM exams WHERE exam_id = ?", examId);
    }

    public List<Exam> findAll() throws SQLException {
        return query(SELECT + "ORDER BY e.exam_date, c.course_code");
    }

    public Optional<Exam> findById(int examId) throws SQLException {
        return queryOne(SELECT + "WHERE e.exam_id = ?", examId);
    }

    public List<Exam> findByFaculty(int facultyId) throws SQLException {
        return query(SELECT + "WHERE c.faculty_id = ? ORDER BY e.exam_date", facultyId);
    }

    /** Exam timetable for the courses a student is enrolled in. */
    public List<Exam> findByStudent(int studentId) throws SQLException {
        return query(SELECT + "JOIN enrollments en ON en.course_id = e.course_id "
                + "WHERE en.student_id = ? ORDER BY e.exam_date", studentId);
    }
}
