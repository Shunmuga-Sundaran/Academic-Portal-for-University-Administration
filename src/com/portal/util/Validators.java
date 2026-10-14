package com.portal.util;

import java.util.Set;
import java.util.regex.Pattern;

/** Server-side validation (the browser also validates, but the server never trusts the browser). */
public final class Validators {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\d{10}$");
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._-]{3,30}$");
    private static final Set<String> PAYMENT_MODES = Set.of("CASH", "UPI", "CARD", "BANK_TRANSFER");

    private Validators() { }

    public static String email(String s) {
        if (!EMAIL.matcher(s).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return s.toLowerCase();
    }

    public static String phone(String s) {
        if (!PHONE.matcher(s).matches()) {
            throw new IllegalArgumentException("Phone must be exactly 10 digits.");
        }
        return s;
    }

    public static String username(String s) {
        String u = s.toLowerCase();
        if (!USERNAME.matcher(u).matches()) {
            throw new IllegalArgumentException("Username must be 3-30 characters: letters, digits, dot, dash or underscore.");
        }
        return u;
    }

    public static String password(String s) {
        if (s.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        return s;
    }

    public static String paymentMode(String s) {
        String m = s.toUpperCase();
        if (!PAYMENT_MODES.contains(m)) {
            throw new IllegalArgumentException("Invalid payment mode.");
        }
        return m;
    }
}
