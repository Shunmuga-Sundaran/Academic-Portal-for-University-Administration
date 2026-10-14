package com.portal.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted PBKDF2 password hashing. Stored format: base64(salt):base64(hash). */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 65_536;
    private static final int KEY_BITS = 256;

    private PasswordUtil() { }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        Base64.Encoder enc = Base64.getEncoder();
        return enc.encodeToString(salt) + ":" + enc.encodeToString(pbkdf2(password, salt));
    }

    public static boolean verify(String password, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        return MessageDigest.isEqual(expected, pbkdf2(password, salt)); // constant-time compare
    }

    private static byte[] pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }
}
