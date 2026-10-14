package com.portal.dao;

import com.portal.config.DBConnection;
import com.portal.model.Role;
import com.portal.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class StudentDAO extends BaseDAO<Student> {

    private static final String SELECT =
            "SELECT s.student_id, s.user_id, s.roll_no, s.name, s.email, s.phone, s.dept_id, "
          + "d.dept_name, s.semester FROM students s JOIN departments d ON s.dept_id = d.dept_id ";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected Student map(ResultSet rs) throws SQLException {
        return new Student(rs.getInt("student_id"), rs.getInt("user_id"), rs.getString("roll_no"),
                rs.getString("name"), rs.getString("email"), rs.getString("phone"),
                rs.getInt("dept_id"), rs.getString("dept_name"), rs.getInt("semester"));
    }

    /** Creates the login account and the student profile in ONE transaction (all or nothing). */
    public void add(Student s, String username, String passwordHash) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId = userDAO.create(con, username, passwordHash, Role.STUDENT);
                String sql = "INSERT INTO students (user_id, roll_no, name, email, phone, dept_id, semester) "
                           + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = Db.prepare(con, sql, userId, s.rollNo(), s.name(),
                        s.email(), s.phone(), s.deptId(), s.semester())) {
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void update(Student s) throws SQLException {
        Db.update("UPDATE students SET name = ?, email = ?, phone = ?, dept_id = ?, semester = ? "
                + "WHERE student_id = ?", s.name(), s.email(), s.phone(), s.deptId(), s.semester(), s.studentId());
    }

    public List<Student> findAll() throws SQLException {
        return query(SELECT + "ORDER BY s.roll_no");
    }

    public List<Student> search(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return query(SELECT + "WHERE s.name LIKE ? OR s.roll_no LIKE ? ORDER BY s.roll_no", like, like);
    }

    public Optional<Student> findById(int studentId) throws SQLException {
        return queryOne(SELECT + "WHERE s.student_id = ?", studentId);
    }

    public Optional<Student> findByRollNo(String rollNo) throws SQLException {
        return queryOne(SELECT + "WHERE s.roll_no = ?", rollNo);
    }

    public Optional<Student> findByUserId(int userId) throws SQLException {
        return queryOne(SELECT + "WHERE s.user_id = ?", userId);
    }

    /** Class roster of a course. */
    public List<Student> findByCourse(int courseId) throws SQLException {
        return query(SELECT + "JOIN enrollments en ON en.student_id = s.student_id "
                + "WHERE en.course_id = ? ORDER BY s.roll_no", courseId);
    }
}
