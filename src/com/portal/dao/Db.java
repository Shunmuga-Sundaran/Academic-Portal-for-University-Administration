package com.portal.dao;

import com.portal.config.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Small JDBC helpers shared by all DAOs. Every query uses PreparedStatement (prevents SQL injection). */
final class Db {

    private Db() { }

    static PreparedStatement prepare(Connection con, String sql, Object... params) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps;
    }

    static int update(String sql, Object... params) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = prepare(con, sql, params)) {
            return ps.executeUpdate();
        }
    }

    static long count(String sql, Object... params) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = prepare(con, sql, params);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
