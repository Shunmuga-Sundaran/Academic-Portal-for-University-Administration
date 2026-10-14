package com.portal.dao;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Row counts for the admin dashboard. */
public class StatsDAO {

    public Map<String, Long> counts() throws SQLException {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("students", Db.count("SELECT COUNT(*) FROM students"));
        m.put("faculty", Db.count("SELECT COUNT(*) FROM faculty"));
        m.put("courses", Db.count("SELECT COUNT(*) FROM courses"));
        m.put("exams", Db.count("SELECT COUNT(*) FROM exams"));
        return m;
    }
}
