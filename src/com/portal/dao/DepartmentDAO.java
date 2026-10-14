package com.portal.dao;

import com.portal.model.Department;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DepartmentDAO extends BaseDAO<Department> {

    @Override
    protected Department map(ResultSet rs) throws SQLException {
        return new Department(rs.getInt("dept_id"), rs.getString("dept_name"));
    }

    public List<Department> findAll() throws SQLException {
        return query("SELECT dept_id, dept_name FROM departments ORDER BY dept_name");
    }

    public void add(String name) throws SQLException {
        Db.update("INSERT INTO departments (dept_name) VALUES (?)", name);
    }
}
