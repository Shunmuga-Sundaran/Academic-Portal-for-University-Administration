package com.portal.web;

import com.portal.dao.CourseDAO;
import com.portal.dao.EnrollmentDAO;
import com.portal.dao.ExamDAO;
import com.portal.dao.FeeDAO;
import com.portal.dao.ResultDAO;
import com.portal.dao.StudentDAO;
import com.portal.model.Course;
import com.portal.model.Fee;
import com.portal.model.ResultRecord;
import com.portal.model.Role;
import com.portal.model.Student;
import com.portal.util.GradeUtil;
import com.portal.util.Validators;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Student portal. The student id ALWAYS comes from the logged-in session, never from the browser,
 * so a student can only ever read or change their own data.
 */
public final class StudentController implements Controller {

    private final StudentDAO studentDAO = new StudentDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollDAO = new EnrollmentDAO();
    private final ExamDAO examDAO = new ExamDAO();
    private final ResultDAO resultDAO = new ResultDAO();
    private final FeeDAO feeDAO = new FeeDAO();

    @Override
    public void register(Router r) {
        Role s = Role.STUDENT;
        r.route("GET", "/api/student/courses", this::catalog, s);
        r.route("POST", "/api/student/enroll", this::enroll, s);
        r.route("POST", "/api/student/drop", this::drop, s);
        r.route("GET", "/api/student/exams", req -> examDAO.findByStudent(me(req).studentId()), s);
        r.route("GET", "/api/student/results", this::results, s);
        r.route("GET", "/api/student/fees", this::fees, s);
        r.route("POST", "/api/student/pay", this::pay, s);
    }

    private Student me(ApiRequest req) throws SQLException {
        return studentDAO.findByUserId(req.user().userId())
                .orElseThrow(() -> ApiException.forbidden("No student profile is linked to this account."));
    }

    /** All courses, each flagged with whether this student is already enrolled. */
    private Object catalog(ApiRequest req) throws SQLException {
        Student me = me(req);
        Set<Integer> mine = new HashSet<>();
        for (Course c : courseDAO.findByStudent(me.studentId())) {
            mine.add(c.courseId());
        }
        return courseDAO.findAll().stream().map(c -> Dto.course(c, mine.contains(c.courseId()))).toList();
    }

    private Object enroll(ApiRequest req) throws SQLException {
        Student me = me(req);
        int courseId = req.integer("courseId");
        if (enrollDAO.isEnrolled(me.studentId(), courseId)) {
            throw new ApiException(409, "You are already enrolled in this course.");
        }
        enrollDAO.enroll(me.studentId(), courseId);
        return "Enrolled successfully.";
    }

    private Object drop(ApiRequest req) throws SQLException {
        if (!enrollDAO.drop(me(req).studentId(), req.integer("courseId"))) {
            throw ApiException.badRequest("You are not enrolled in that course.");
        }
        return "Course dropped.";
    }

    private Object results(ApiRequest req) throws SQLException {
        List<ResultRecord> list = resultDAO.findByStudent(me(req).studentId());
        Double overall = null;
        String grade = null;
        if (!list.isEmpty()) {
            double avg = list.stream().mapToDouble(ResultRecord::percentage).average().orElse(0);
            overall = Math.round(avg * 100) / 100.0;
            grade = GradeUtil.grade(avg);
        }
        return Dto.map("results", Dto.results(list), "overall", overall, "grade", grade);
    }

    private Object fees(ApiRequest req) throws SQLException {
        Student me = me(req);
        List<Fee> fees = feeDAO.findByStudent(me.studentId());
        BigDecimal outstanding = fees.stream().map(Fee::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dto.map("fees", Dto.fees(fees), "payments", feeDAO.findPaymentsByStudent(me.studentId()),
                "outstanding", outstanding);
    }

    private Object pay(ApiRequest req) throws SQLException {
        Student me = me(req);
        int feeId = req.integer("feeId");
        // ownership check: the fee must be in THIS student's own list
        boolean mine = feeDAO.findByStudent(me.studentId()).stream().anyMatch(f -> f.feeId() == feeId);
        if (!mine) {
            throw ApiException.forbidden("That fee does not belong to you.");
        }
        feeDAO.recordPayment(feeId, req.amount("amount"), Validators.paymentMode(req.str("mode")));
        return "Payment recorded. Thank you!";
    }
}
