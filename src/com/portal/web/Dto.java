package com.portal.web;

import com.portal.model.Course;
import com.portal.model.Fee;
import com.portal.model.ResultRecord;
import com.portal.model.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the exact JSON shape sent to the browser (e.g. the password hash is never included). */
final class Dto {

    private Dto() { }

    /** map("a", 1, "b", 2) -> {a:1, b:2}. Unlike Map.of it allows null values. */
    static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    static Map<String, Object> user(User u) {
        return map("userId", u.userId(), "username", u.username(), "role", u.role().name(), "active", u.active());
    }

    static List<Map<String, Object>> users(List<User> list) {
        return list.stream().map(Dto::user).toList();
    }

    static Map<String, Object> fee(Fee f) {
        return map("feeId", f.feeId(), "studentId", f.studentId(), "rollNo", f.rollNo(),
                "studentName", f.studentName(), "description", f.description(), "amount", f.amount(),
                "paid", f.paid(), "balance", f.balance(), "dueDate", f.dueDate(), "status", f.status());
    }

    static List<Map<String, Object>> fees(List<Fee> list) {
        return list.stream().map(Dto::fee).toList();
    }

    static Map<String, Object> result(ResultRecord r) {
        return map("examId", r.examId(), "studentId", r.studentId(), "rollNo", r.rollNo(),
                "studentName", r.studentName(), "courseCode", r.courseCode(), "examName", r.examName(),
                "marks", r.marks(), "maxMarks", r.maxMarks(),
                "percentage", Math.round(r.percentage() * 10) / 10.0, "grade", r.grade());
    }

    static List<Map<String, Object>> results(List<ResultRecord> list) {
        return list.stream().map(Dto::result).toList();
    }

    static Map<String, Object> course(Course c, boolean enrolled) {
        return map("courseId", c.courseId(), "courseCode", c.courseCode(), "courseName", c.courseName(),
                "credits", c.credits(), "deptName", c.deptName(), "facultyName", c.facultyName(),
                "enrolled", enrolled);
    }
}
