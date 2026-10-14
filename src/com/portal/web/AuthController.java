package com.portal.web;

import com.portal.dao.FacultyDAO;
import com.portal.dao.StudentDAO;
import com.portal.model.Faculty;
import com.portal.model.Student;
import com.portal.model.User;
import com.portal.service.AuthService;
import com.portal.util.Validators;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Login, logout, "who am I" and change-password. */
public final class AuthController implements Controller {

    private final SessionManager sessions;
    private final AuthService auth = new AuthService();
    private final StudentDAO studentDAO = new StudentDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final Throttle throttle = new Throttle();

    public AuthController(SessionManager sessions) {
        this.sessions = sessions;
    }

    @Override
    public void register(Router r) {
        r.publicRoute("POST", "/api/login", this::login);
        r.route("POST", "/api/logout", this::logout);
        r.route("GET", "/api/me", req -> profile(req.user()));
        r.route("POST", "/api/password", this::changePassword);
    }

    private Object login(ApiRequest req) throws SQLException {
        String username = req.str("username").toLowerCase();
        if (throttle.isLocked(username)) {
            throw new ApiException(429, "Too many failed attempts. Try again in 5 minutes.");
        }
        Optional<User> user = auth.login(username, req.str("password"));
        if (user.isEmpty()) {
            throttle.recordFailure(username);
            throw new ApiException(401, "Invalid username or password.");
        }
        throttle.clear(username);
        String token = sessions.create(user.get().userId());
        req.addResponseCookie(SessionManager.cookieHeader(token, 1800));
        return profile(user.get());
    }

    private Object logout(ApiRequest req) {
        sessions.destroy(req.sessionToken());
        req.addResponseCookie(SessionManager.cookieHeader("", 0));
        return "Logged out.";
    }

    private Object changePassword(ApiRequest req) throws SQLException {
        String newPassword = Validators.password(req.str("newPassword"));
        if (!auth.changePassword(req.user(), req.str("oldPassword"), newPassword)) {
            throw ApiException.badRequest("Current password is incorrect.");
        }
        return "Password changed successfully.";
    }

    private Map<String, Object> profile(User u) throws SQLException {
        String name = "Administrator";
        Object details = null;
        switch (u.role()) {
            case STUDENT -> {
                Student s = studentDAO.findByUserId(u.userId()).orElse(null);
                name = s == null ? u.username() : s.name();
                details = s;
            }
            case FACULTY -> {
                Faculty f = facultyDAO.findByUserId(u.userId()).orElse(null);
                name = f == null ? u.username() : f.name();
                details = f;
            }
            default -> { }
        }
        return Dto.map("username", u.username(), "role", u.role().name(), "name", name, "details", details);
    }

    /** Blocks a username for 5 minutes after 5 wrong passwords (simple brute-force protection). */
    private static final class Throttle {
        private static final int MAX_FAILURES = 5;
        private static final long LOCK_MS = 5 * 60 * 1000L;
        private final Map<String, Integer> failures = new HashMap<>();
        private final Map<String, Long> lockedUntil = new HashMap<>();

        synchronized boolean isLocked(String username) {
            Long until = lockedUntil.get(username);
            if (until == null) {
                return false;
            }
            if (until < System.currentTimeMillis()) {
                clear(username);
                return false;
            }
            return true;
        }

        synchronized void recordFailure(String username) {
            int n = failures.merge(username, 1, Integer::sum);
            if (n >= MAX_FAILURES) {
                lockedUntil.put(username, System.currentTimeMillis() + LOCK_MS);
            }
        }

        synchronized void clear(String username) {
            failures.remove(username);
            lockedUntil.remove(username);
        }
    }
}
