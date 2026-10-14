package com.portal.dao;

import com.portal.model.Course;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CourseDAO extends BaseDAO<Course> {

    private static final String SELECT =
            "SELECT c.course_id, c.course_code, c.course_name, c.credits, c.dept_id, d.dept_name, "
          + "c.faculty_id, f.name AS faculty_name FROM courses c "
          + "JOIN departments d ON c.dept_id = d.dept_id "
          + "LEFT JOIN faculty f ON c.faculty_id = f.faculty_id ";

    @Override
    protected Course map(ResultSet rs) throws SQLException {
        int fid = rs.getInt("faculty_id");
        Integer facultyId = rs.wasNull() ? null : fid;
        return new Course(rs.getInt("course_id"), rs.getString("course_code"), rs.getString("course_name"),
                rs.getInt("credits"), rs.getInt("dept_id"), rs.getString("dept_name"),
                facultyId, rs.getString("faculty_name"));
    }

    public void add(String code, String name, int credits, int deptId, Integer facultyId) throws SQLException {
        Db.update("INSERT INTO courses (course_code, course_name, credits, dept_id, faculty_id) VALUES (?, ?, ?, ?, ?)",
                code, name, credits, deptId, facultyId);
    }

    public void update(int courseId, String name, int credits, int deptId) throws SQLException {
        Db.update("UPDATE courses SET course_name = ?, credits = ?, dept_id = ? WHERE course_id = ?",
                name, credits, deptId, courseId);
    }

    public void assignFaculty(int courseId, Integer facultyId) throws SQLException {
        Db.update("UPDATE courses SET faculty_id = ? WHERE course_id = ?", facultyId, courseId);
    }

    public void delete(int courseId) throws SQLException {
        Db.update("DELETE FROM courses WHERE course_id = ?", courseId);
    }

    public List<Course> findAll() throws SQLException {
        return query(SELECT + "ORDER BY c.course_code");
    }

    public Optional<Course> findByCode(String code) throws SQLException {
        return queryOne(SELECT + "WHERE c.course_code = ?", code);
    }

    public List<Course> findByFaculty(int facultyId) throws SQLException {
        return query(SELECT + "WHERE c.faculty_id = ? ORDER BY c.course_code", facultyId);
    }

    public List<Course> findByStudent(int studentId) throws SQLException {
        return query(SELECT + "JOIN enrollments en ON en.course_id = c.course_id "
                + "WHERE en.student_id = ? ORDER BY c.course_code", studentId);
    }
}
