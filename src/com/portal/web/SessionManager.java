package com.portal.web;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side login sessions. The browser only holds a random token in an HttpOnly cookie. */
public final class SessionManager {

    public static final String COOKIE_NAME = "PORTAL_SESSION";
    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes of inactivity

    private static final class Session {
        final int userId;
        volatile long lastAccess = System.currentTimeMillis();

        Session(int userId) {
            this.userId = userId;
        }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String create(int userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(userId));
        return token;
    }

    /** Returns the user id for a valid token (and refreshes its timeout), otherwise null. */
    public Integer userIdFor(String token) {
        if (token == null) {
            return null;
        }
        Session s = sessions.get(token);
        if (s == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - s.lastAccess > TIMEOUT_MS) {
            sessions.remove(token);
            return null;
        }
        s.lastAccess = now;
        return s.userId;
    }

    public void destroy(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /** Cookie header value. HttpOnly = JavaScript cannot read it; SameSite=Strict = no cross-site sends. */
    public static String cookieHeader(String token, int maxAgeSeconds) {
        return COOKIE_NAME + "=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + maxAgeSeconds;
    }
}
