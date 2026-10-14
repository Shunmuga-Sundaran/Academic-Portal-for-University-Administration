package com.portal.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Creates JDBC connections using the details in db.properties (kept out of Git). */
public final class DBConnection {

    private static Properties props;

    private DBConnection() { }

    private static synchronized Properties load() throws SQLException {
        if (props == null) {
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream("db.properties")) {
                p.load(in);
            } catch (IOException e) {
                throw new SQLException("Cannot read db.properties in " + System.getProperty("user.dir")
                        + ". Copy db.properties.example to db.properties and enter your MySQL details.", e);
            }
            props = p;
        }
        return props;
    }

    public static Connection getConnection() throws SQLException {
        Properties p = load();
        return DriverManager.getConnection(
                p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.password"));
    }
}
