package com.portal.model;

import com.portal.util.GradeUtil;
import java.math.BigDecimal;

public record ResultRecord(int examId, int studentId, String rollNo, String studentName,
                           String courseCode, String examName, BigDecimal marks, int maxMarks) {

    public double percentage() {
        return marks.doubleValue() * 100.0 / maxMarks;
    }

    public String grade() {
        return GradeUtil.grade(percentage());
    }
}
