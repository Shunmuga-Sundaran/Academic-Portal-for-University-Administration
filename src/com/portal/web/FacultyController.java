package com.portal.web;

import com.portal.dao.CourseDAO;
import com.portal.dao.ExamDAO;
import com.portal.dao.FacultyDAO;
import com.portal.dao.ResultDAO;
import com.portal.dao.StudentDAO;
import com.portal.model.Course;
import com.portal.model.Exam;
import com.portal.model.Faculty;
import com.portal.model.ResultRecord;
import com.portal.model.Role;
import com.portal.model.Student;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Faculty portal. Beyond the role check, every action verifies OWNERSHIP: a faculty member can
 * only work with courses/exams that belong to them, even if they guess another id.
 */
public final class FacultyController implements Controller {

    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final ExamDAO examDAO = new ExamDAO();
    private final ResultDAO resultDAO = new ResultDAO();

    @Override
    public void register(Router r) {
        Role f = Role.FACULTY;
        r.route("GET", "/api/faculty/courses", req -> courseDAO.findByFaculty(me(req).facultyId()), f);
        r.route("GET", "/api/faculty/roster", this::roster, f);
        r.route("GET", "/api/faculty/exams", req -> examDAO.findByFaculty(me(req).facultyId()), f);
        r.route("POST", "/api/faculty/exams", this::scheduleExam, f);
        r.route("GET", "/api/faculty/marks", this::marksSheet, f);
        r.route("POST", "/api/faculty/marks", this::saveMarks, f);
        r.route("GET", "/api/faculty/results", this::results, f);
    }

    private Faculty me(ApiRequest req) throws SQLException {
        return facultyDAO.findByUserId(req.user().userId())
                .orElseThrow(() -> ApiException.forbidden("No faculty profile is linked to this account."));
    }

    private Course courseOf(Faculty me, int courseId) throws SQLException {
        return courseDAO.findByFaculty(me.facultyId()).stream()
                .filter(c -> c.courseId() == courseId).findFirst()
                .orElseThrow(() -> ApiException.forbidden("That is not one of your courses."));
    }

    private Exam examOf(Faculty me, int examId) throws SQLException {
        return examDAO.findByFaculty(me.facultyId()).stream()
                .filter(e -> e.examId() == examId).findFirst()
                .orElseThrow(() -> ApiException.forbidden("That is not one of your exams."));
    }

    private Object roster(ApiRequest req) throws SQLException {
        Course c = courseOf(me(req), req.integer("courseId"));
        return studentDAO.findByCourse(c.courseId());
    }

    private Object scheduleExam(ApiRequest req) throws SQLException {
        Course c = courseOf(me(req), req.integer("courseId"));
        examDAO.add(c.courseId(), req.str("examName"), req.date("examDate"), req.integer("maxMarks", 1, 500));
        return "Exam scheduled.";
    }

    /** The mark-entry grid: every enrolled student with the marks already saved (or null). */
    private Object marksSheet(ApiRequest req) throws SQLException {
        Exam exam = examOf(me(req), req.integer("examId"));
        Map<Integer, BigDecimal> saved = new HashMap<>();
        for (ResultRecord r : resultDAO.findByExam(exam.examId())) {
            saved.put(r.studentId(), r.marks());
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Student s : studentDAO.findByCourse(exam.courseId())) {
            rows.add(Dto.map("studentId", s.studentId(), "rollNo", s.rollNo(), "name", s.name(),
                    "marks", saved.get(s.studentId())));
        }
        return Dto.map("exam", exam, "rows", rows);
    }

    /** entries = "studentId:marks,studentId:marks"; blank marks are skipped. */
    private Object saveMarks(ApiRequest req) throws SQLException {
        Exam exam = examOf(me(req), req.integer("examId"));
        Set<Integer> enrolled = new HashSet<>();
        for (Student s : studentDAO.findByCourse(exam.courseId())) {
            enrolled.add(s.studentId());
        }
        String entries = req.opt("entries");
        if (entries == null) {
            throw ApiException.badRequest("Enter marks for at least one student.");
        }
        int saved = 0;
        for (String pair : entries.split(",")) {
            String[] p = pair.split(":");
            if (p.length != 2 || p[1].isBlank()) {
                continue;
            }
            try {
                int studentId = Integer.parseInt(p[0].trim());
                BigDecimal marks = new BigDecimal(p[1].trim()).setScale(2, RoundingMode.HALF_UP);
                if (!enrolled.contains(studentId)) {
                    throw ApiException.badRequest("Student " + studentId + " is not enrolled in this course.");
                }
                if (marks.signum() < 0 || marks.compareTo(BigDecimal.valueOf(exam.maxMarks())) > 0) {
                    throw ApiException.badRequest("Marks must be between 0 and " + exam.maxMarks() + ".");
                }
                resultDAO.save(exam.examId(), studentId, marks);
                saved++;
            } catch (NumberFormatException e) {
                throw ApiException.badRequest("Invalid marks value: " + pair);
            }
        }
        return "Marks saved for " + saved + " student(s).";
    }

    private Object results(ApiRequest req) throws SQLException {
        Exam exam = examOf(me(req), req.integer("examId"));
        List<ResultRecord> list = resultDAO.findByExam(exam.examId());
        Double average = list.isEmpty() ? null
                : Math.round(list.stream().mapToDouble(ResultRecord::percentage).average().orElse(0) * 10) / 10.0;
        long passed = list.stream().filter(r -> r.percentage() >= 40).count();
        return Dto.map("exam", exam, "results", Dto.results(list), "average", average,
                "passed", passed, "total", list.size());
    }
}
