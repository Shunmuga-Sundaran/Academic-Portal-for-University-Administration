package com.portal;

import com.portal.dao.UserDAO;
import com.portal.model.Role;
import com.portal.util.PasswordUtil;
import com.portal.web.AdminController;
import com.portal.web.AuthController;
import com.portal.web.Controller;
import com.portal.web.FacultyController;
import com.portal.web.Router;
import com.portal.web.SessionManager;
import com.portal.web.StaticHandler;
import com.portal.web.StudentController;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Entry point. Starts a web server on http://localhost:8080 that
 *   - serves the HTML/CSS/JavaScript pages from the web/ folder, and
 *   - answers the /api/... calls the pages make (Java + JDBC + MySQL).
 */
public class WebApp {

    private static final int PORT = 8080;
    private static final String DEFAULT_ADMIN = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    public static void main(String[] args) throws IOException {
        try {
            createDefaultAdminIfNeeded();
        } catch (SQLException e) {
            System.out.println("Could not connect to the database: " + e.getMessage());
            System.out.println("Check: MySQL is running, sql/schema.sql was executed, db.properties is correct,");
            System.out.println("and the MySQL Connector/J jar is inside the lib folder.");
            return;
        }

        Path webRoot = Paths.get("web").toAbsolutePath().normalize();
        if (!Files.isDirectory(webRoot)) {
            System.out.println("Cannot find the web folder: " + webRoot);
            System.out.println("Run the app from the project root folder (the one that contains web/ and src/).");
            return;
        }

        SessionManager sessions = new SessionManager();
        Router router = new Router(sessions);
        List<Controller> controllers = List.of(
                new AuthController(sessions), new AdminController(),
                new FacultyController(), new StudentController());
        for (Controller c : controllers) {
            c.register(router); // polymorphism: each controller adds its own URLs
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/api/", router);
        server.createContext("/", new StaticHandler(webRoot));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

        System.out.println("=================================================");
        System.out.println("  Academic Portal is running");
        System.out.println("  Open in your browser:  http://localhost:" + PORT);
        System.out.println("  Press Ctrl+C to stop.");
        System.out.println("=================================================");
    }

    /** First run only: creates an admin so someone can log in and add everyone else. */
    private static void createDefaultAdminIfNeeded() throws SQLException {
        UserDAO userDAO = new UserDAO();
        if (userDAO.count() == 0) {
            userDAO.create(DEFAULT_ADMIN, PasswordUtil.hash(DEFAULT_ADMIN_PASSWORD), Role.ADMIN);
            System.out.println("First run: default admin created -> username: " + DEFAULT_ADMIN
                    + "  password: " + DEFAULT_ADMIN_PASSWORD + "  (change it after login)");
        }
    }
}
