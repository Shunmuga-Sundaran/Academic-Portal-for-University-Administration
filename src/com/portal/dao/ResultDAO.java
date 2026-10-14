package com.portal.dao;

import com.portal.model.ResultRecord;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ResultDAO extends BaseDAO<ResultRecord> {

    private static final String SELECT =
            "SELECT r.exam_id, r.student_id, s.roll_no, s.name, c.course_code, e.exam_name, "
          + "r.marks_obtained, e.max_marks FROM results r "
          + "JOIN exams e ON r.exam_id = e.exam_id "
          + "JOIN courses c ON e.course_id = c.course_id "
          + "JOIN students s ON r.student_id = s.student_id ";

    @Override
    protected ResultRecord map(ResultSet rs) throws SQLException {
        return new ResultRecord(rs.getInt("exam_id"), rs.getInt("student_id"), rs.getString("roll_no"),
                rs.getString("name"), rs.getString("course_code"), rs.getString("exam_name"),
                rs.getBigDecimal("marks_obtained"), rs.getInt("max_marks"));
    }

    /** Insert new marks, or update them if already entered (UNIQUE exam+student). */
    public void save(int examId, int studentId, BigDecimal marks) throws SQLException {
        Db.update("INSERT INTO results (exam_id, student_id, marks_obtained) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE marks_obtained = ?", examId, studentId, marks, marks);
    }

    public List<ResultRecord> findByStudent(int studentId) throws SQLException {
        return query(SELECT + "WHERE r.student_id = ? ORDER BY c.course_code, e.exam_date", studentId);
    }

    public List<ResultRecord> findByExam(int examId) throws SQLException {
        return query(SELECT + "WHERE r.exam_id = ? ORDER BY s.roll_no", examId);
    }
}
