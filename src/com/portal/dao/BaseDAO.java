package com.portal.dao;

import com.portal.config.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Generic DAO: subclasses only say how to turn one ResultSet row into an object (map). */
abstract class BaseDAO<T> {

    protected abstract T map(ResultSet rs) throws SQLException;

    protected List<T> query(String sql, Object... params) throws SQLException {
        List<T> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = Db.prepare(con, sql, params);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    protected Optional<T> queryOne(String sql, Object... params) throws SQLException {
        List<T> list = query(sql, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
