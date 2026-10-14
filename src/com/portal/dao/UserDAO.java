package com.portal.dao;

import com.portal.config.DBConnection;
import com.portal.model.Role;
import com.portal.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public class UserDAO extends BaseDAO<User> {

    private static final String SELECT =
            "SELECT user_id, username, password_hash, role_id, is_active FROM users ";

    @Override
    protected User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("password_hash"),
                Role.fromId(rs.getInt("role_id")), rs.getBoolean("is_active"));
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        return queryOne(SELECT + "WHERE username = ?", username);
    }

    public Optional<User> findById(int userId) throws SQLException {
        return queryOne(SELECT + "WHERE user_id = ?", userId);
    }

    public List<User> findAll() throws SQLException {
        return query(SELECT + "ORDER BY username");
    }

    public long count() throws SQLException {
        return Db.count("SELECT COUNT(*) FROM users");
    }

    /** Used inside a transaction (profile row is inserted with the same connection). */
    public int create(Connection con, String username, String hash, Role role) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.setInt(3, role.getId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public int create(String username, String hash, Role role) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            return create(con, username, hash, role);
        }
    }

    public void updatePassword(int userId, String hash) throws SQLException {
        Db.update("UPDATE users SET password_hash = ? WHERE user_id = ?", hash, userId);
    }

    public void setActive(int userId, boolean active) throws SQLException {
        Db.update("UPDATE users SET is_active = ? WHERE user_id = ?", active, userId);
    }

    /** Deleting the user cascades to the student/faculty profile and everything linked to it. */
    public void delete(int userId) throws SQLException {
        Db.update("DELETE FROM users WHERE user_id = ?", userId);
    }
}
