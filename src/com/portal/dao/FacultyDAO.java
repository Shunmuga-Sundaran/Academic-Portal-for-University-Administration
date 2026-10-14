package com.portal.dao;

import com.portal.config.DBConnection;
import com.portal.model.Faculty;
import com.portal.model.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class FacultyDAO extends BaseDAO<Faculty> {

    private static final String SELECT =
            "SELECT fa.faculty_id, fa.user_id, fa.name, fa.email, fa.dept_id, d.dept_name, fa.designation "
          + "FROM faculty fa JOIN departments d ON fa.dept_id = d.dept_id ";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected Faculty map(ResultSet rs) throws SQLException {
        return new Faculty(rs.getInt("faculty_id"), rs.getInt("user_id"), rs.getString("name"),
                rs.getString("email"), rs.getInt("dept_id"), rs.getString("dept_name"),
                rs.getString("designation"));
    }

    public void add(Faculty f, String username, String passwordHash) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId = userDAO.create(con, username, passwordHash, Role.FACULTY);
                String sql = "INSERT INTO faculty (user_id, name, email, dept_id, designation) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = Db.prepare(con, sql, userId, f.name(), f.email(),
                        f.deptId(), f.designation())) {
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void update(Faculty f) throws SQLException {
        Db.update("UPDATE faculty SET name = ?, email = ?, dept_id = ?, designation = ? WHERE faculty_id = ?",
                f.name(), f.email(), f.deptId(), f.designation(), f.facultyId());
    }

    public List<Faculty> findAll() throws SQLException {
        return query(SELECT + "ORDER BY fa.name");
    }

    public Optional<Faculty> findById(int facultyId) throws SQLException {
        return queryOne(SELECT + "WHERE fa.faculty_id = ?", facultyId);
    }

    public Optional<Faculty> findByUserId(int userId) throws SQLException {
        return queryOne(SELECT + "WHERE fa.user_id = ?", userId);
    }
}
