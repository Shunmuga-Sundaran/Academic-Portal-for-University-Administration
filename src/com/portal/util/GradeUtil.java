package com.portal.util;

/** Converts a percentage to a letter grade. */
public final class GradeUtil {

    private GradeUtil() { }

    public static String grade(double percentage) {
        if (percentage >= 90) return "O";
        if (percentage >= 80) return "A+";
        if (percentage >= 70) return "A";
        if (percentage >= 60) return "B+";
        if (percentage >= 50) return "B";
        if (percentage >= 40) return "C";
        return "F";
    }
}
