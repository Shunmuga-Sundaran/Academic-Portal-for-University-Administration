package com.portal.web;

import com.portal.model.User;
import com.sun.net.httpserver.HttpExchange;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/** One incoming API call: its parameters (query string + form body) and the logged-in user. */
public final class ApiRequest {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private final HttpExchange exchange;
    private final Map<String, List<String>> params;
    private final User user;
    private final String sessionToken;

    ApiRequest(HttpExchange exchange, Map<String, List<String>> params, User user, String sessionToken) {
        this.exchange = exchange;
        this.params = params;
        this.user = user;
        this.sessionToken = sessionToken;
    }

    public User user() {
        return user;
    }

    public String sessionToken() {
        return sessionToken;
    }

    public void addResponseCookie(String setCookieHeader) {
        exchange.getResponseHeaders().add("Set-Cookie", setCookieHeader);
    }

    /** Optional text parameter: null when missing or blank. */
    public String opt(String name) {
        List<String> values = params.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        String s = values.get(0).trim();
        return s.isEmpty() ? null : s;
    }

    public String str(String name) {
        String s = opt(name);
        if (s == null) {
            throw ApiException.badRequest("Missing required field: " + name);
        }
        return s;
    }

    public int integer(String name) {
        String s = str(name);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(name + " must be a whole number.");
        }
    }

    public int integer(String name, int min, int max) {
        int v = integer(name);
        if (v < min || v > max) {
            throw ApiException.badRequest(name + " must be between " + min + " and " + max + ".");
        }
        return v;
    }

    public Integer optInteger(String name) {
        return opt(name) == null ? null : integer(name);
    }

    public BigDecimal amount(String name) {
        String s = str(name);
        try {
            BigDecimal v = new BigDecimal(s);
            if (v.signum() <= 0 || v.compareTo(MAX_AMOUNT) > 0) {
                throw ApiException.badRequest(name + " must be greater than 0 and at most 99999999.99.");
            }
            return v.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(name + " must be a number.");
        }
    }

    public LocalDate date(String name) {
        try {
            return LocalDate.parse(str(name));
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest(name + " must be a date in the format yyyy-MM-dd.");
        }
    }
}
