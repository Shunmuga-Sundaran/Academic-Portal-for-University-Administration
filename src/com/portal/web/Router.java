package com.portal.web;

import com.portal.dao.UserDAO;
import com.portal.model.Role;
import com.portal.model.User;
import com.portal.service.AccessControl;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Front controller for every /api/... URL. For each request it: finds the route, checks the
 * session, checks the role (RBAC), parses parameters, runs the action and writes JSON.
 */
public final class Router implements HttpHandler {

    @FunctionalInterface
    public interface Action {
        Object handle(ApiRequest request) throws Exception;
    }

    private record Route(Action action, boolean isPublic, Role[] roles) { }

    private static final int MAX_BODY_BYTES = 64 * 1024;

    private final Map<String, Route> routes = new HashMap<>();
    private final SessionManager sessions;
    private final UserDAO userDAO = new UserDAO();

    public Router(SessionManager sessions) {
        this.sessions = sessions;
    }

    /** A URL anyone can call (only login). */
    public void publicRoute(String method, String path, Action action) {
        routes.put(method + " " + path, new Route(action, true, new Role[0]));
    }

    /** A URL for logged-in users. If roles are given, only those roles may call it. */
    public void route(String method, String path, Action action, Role... allowedRoles) {
        routes.put(method + " " + path, new Route(action, false, allowedRoles));
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        int status = 200;
        Object body;
        try {
            body = dispatch(ex);
        } catch (ApiException e) {
            status = e.status();
            body = Dto.map("error", e.getMessage());
        } catch (SecurityException e) {
            status = 403;
            body = Dto.map("error", e.getMessage());
        } catch (SQLIntegrityConstraintViolationException e) {
            status = 409;
            body = Dto.map("error", "Duplicate value, invalid reference, or the record is still in use.");
        } catch (SQLException e) {
            e.printStackTrace();
            status = 500;
            body = Dto.map("error", "Database error. Check the server console.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            status = 400;
            body = Dto.map("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            status = 500;
            body = Dto.map("error", "Unexpected server error.");
        }
        send(ex, status, body);
    }

    private Object dispatch(HttpExchange ex) throws Exception {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        Route route = routes.get(method + " " + path);
        if (route == null) {
            throw new ApiException(404, "Unknown endpoint: " + method + " " + path);
        }
        String token = cookie(ex, SessionManager.COOKIE_NAME);
        User user = null;
        if (!route.isPublic()) {
            user = authenticate(token);
            if (route.roles().length > 0) {
                AccessControl.require(user, route.roles()); // RBAC: wrong role -> 403
            }
        }
        ApiRequest request = new ApiRequest(ex, parseParams(ex), user, token);
        Object result = route.action().handle(request);
        if (result == null) {
            return Dto.map("message", "OK");
        }
        if (result instanceof String message) {
            return Dto.map("message", message);
        }
        return result;
    }

    /** Valid session AND the account must still exist and be active (checked on every request). */
    private User authenticate(String token) throws SQLException {
        Integer userId = sessions.userIdFor(token);
        if (userId == null) {
            throw new ApiException(401, "Please log in.");
        }
        User user = userDAO.findById(userId).orElse(null);
        if (user == null || !user.active()) {
            sessions.destroy(token);
            throw new ApiException(401, "Session expired. Please log in again.");
        }
        return user;
    }

    private static String cookie(HttpExchange ex, String name) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) {
            return null;
        }
        for (String header : headers) {
            for (String part : header.split(";")) {
                String p = part.trim();
                if (p.startsWith(name + "=")) {
                    return p.substring(name.length() + 1);
                }
            }
        }
        return null;
    }

    private static Map<String, List<String>> parseParams(HttpExchange ex) throws IOException {
        Map<String, List<String>> params = new LinkedHashMap<>();
        addPairs(params, ex.getRequestURI().getRawQuery());
        String method = ex.getRequestMethod();
        if (method.equals("POST") || method.equals("PUT")) {
            byte[] bytes = ex.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                throw new ApiException(413, "Request is too large.");
            }
            addPairs(params, new String(bytes, StandardCharsets.UTF_8));
        }
        return params;
    }

    private static void addPairs(Map<String, List<String>> params, String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return;
        }
        for (String pair : encoded.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            try {
                String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("Malformed request parameters.");
            }
        }
    }

    private static void send(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = Json.write(body).getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("Cache-Control", "no-store");
        h.set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
