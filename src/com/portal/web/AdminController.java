package com.portal.web;

import com.portal.dao.CourseDAO;
import com.portal.dao.DepartmentDAO;
import com.portal.dao.EnrollmentDAO;
import com.portal.dao.ExamDAO;
import com.portal.dao.FacultyDAO;
import com.portal.dao.FeeDAO;
import com.portal.dao.ResultDAO;
import com.portal.dao.StatsDAO;
import com.portal.dao.StudentDAO;
import com.portal.dao.UserDAO;
import com.portal.model.Faculty;
import com.portal.model.Role;
import com.portal.model.Student;
import com.portal.model.User;
import com.portal.util.PasswordUtil;
import com.portal.util.Validators;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

/** Everything the administrator can do. Every route below is registered for Role.ADMIN only. */
public final class AdminController implements Controller {

    private final DepartmentDAO deptDAO = new DepartmentDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollDAO = new EnrollmentDAO();
    private final ExamDAO examDAO = new ExamDAO();
    private final ResultDAO resultDAO = new ResultDAO();
    private final FeeDAO feeDAO = new FeeDAO();
    private final UserDAO userDAO = new UserDAO();
    private final StatsDAO statsDAO = new StatsDAO();

    @Override
    public void register(Router r) {
        Role a = Role.ADMIN;
        r.route("GET", "/api/admin/stats", this::stats, a);

        r.route("GET", "/api/admin/departments", req -> deptDAO.findAll(), a);
        r.route("POST", "/api/admin/departments", this::addDepartment, a);

        r.route("GET", "/api/admin/students", this::listStudents, a);
        r.route("POST", "/api/admin/students", this::addStudent, a);
        r.route("PUT", "/api/admin/students", this::updateStudent, a);
        r.route("DELETE", "/api/admin/students", this::deleteStudent, a);

        r.route("GET", "/api/admin/faculty", req -> facultyDAO.findAll(), a);
        r.route("POST", "/api/admin/faculty", this::addFaculty, a);
        r.route("PUT", "/api/admin/faculty", this::updateFaculty, a);
        r.route("DELETE", "/api/admin/faculty", this::deleteFaculty, a);

        r.route("GET", "/api/admin/courses", req -> courseDAO.findAll(), a);
        r.route("POST", "/api/admin/courses", this::addCourse, a);
        r.route("PUT", "/api/admin/courses", this::updateCourse, a);
        r.route("DELETE", "/api/admin/courses", this::deleteCourse, a);

        r.route("GET", "/api/admin/enrollments", req -> studentDAO.findByCourse(req.integer("courseId")), a);
        r.route("POST", "/api/admin/enrollments", this::enroll, a);
        r.route("DELETE", "/api/admin/enrollments", this::drop, a);

        r.route("GET", "/api/admin/exams", req -> examDAO.findAll(), a);
        r.route("POST", "/api/admin/exams", this::scheduleExam, a);
        r.route("DELETE", "/api/admin/exams", this::deleteExam, a);
        r.route("GET", "/api/admin/results", req -> Dto.results(resultDAO.findByExam(req.integer("examId"))), a);

        r.route("GET", "/api/admin/fees/summary", req -> feeSummary(), a);
        r.route("GET", "/api/admin/fees/outstanding", req -> Dto.fees(feeDAO.findOutstanding()), a);
        r.route("GET", "/api/admin/fees/ledger", this::ledger, a);
        r.route("POST", "/api/admin/fees", this::addFee, a);
        r.route("POST", "/api/admin/fees/all", this::addFeeForAll, a);
        r.route("POST", "/api/admin/payments", this::recordPayment, a);

        r.route("GET", "/api/admin/users", req -> Dto.users(userDAO.findAll()), a);
        r.route("POST", "/api/admin/users/reset", this::resetPassword, a);
        r.route("POST", "/api/admin/users/toggle", this::toggleUser, a);
    }

    // ---------------- dashboard & departments ----------------

    private Object stats(ApiRequest req) throws SQLException {
        Map<String, Object> m = Dto.map();
        m.putAll(statsDAO.counts());
        m.putAll(feeSummary());
        return m;
    }

    private Object addDepartment(ApiRequest req) throws SQLException {
        deptDAO.add(req.str("name"));
        return "Department added.";
    }

    // ---------------- students ----------------

    private Object listStudents(ApiRequest req) throws SQLException {
        String q = req.opt("q");
        return q == null ? studentDAO.findAll() : studentDAO.search(q);
    }

    private Object addStudent(ApiRequest req) throws SQLException {
        String roll = req.str("rollNo").toUpperCase();
        String username = roll.toLowerCase();
        String password = Validators.password(req.str("password"));
        Student s = new Student(0, 0, roll, req.str("name"), Validators.email(req.str("email")),
                Validators.phone(req.str("phone")), req.integer("deptId"), null, req.integer("semester", 1, 8));
        studentDAO.add(s, username, PasswordUtil.hash(password));
        return "Student added. Login username: " + username;
    }

    private Object updateStudent(ApiRequest req) throws SQLException {
        Student old = studentDAO.findById(req.integer("id"))
                .orElseThrow(() -> ApiException.notFound("Student not found."));
        studentDAO.update(new Student(old.studentId(), old.userId(), old.rollNo(), req.str("name"),
                Validators.email(req.str("email")), Validators.phone(req.str("phone")),
                req.integer("deptId"), null, req.integer("semester", 1, 8)));
        return "Student updated.";
    }

    private Object deleteStudent(ApiRequest req) throws SQLException {
        Student s = studentDAO.findById(req.integer("id"))
                .orElseThrow(() -> ApiException.notFound("Student not found."));
        userDAO.delete(s.userId()); // cascades to enrollments, marks and fees
        return "Student deleted.";
    }

    // ---------------- faculty ----------------

    private Object addFaculty(ApiRequest req) throws SQLException {
        String username = Validators.username(req.str("username"));
        String password = Validators.password(req.str("password"));
        Faculty f = new Faculty(0, 0, req.str("name"), Validators.email(req.str("email")),
                req.integer("deptId"), null, req.str("designation"));
        facultyDAO.add(f, username, PasswordUtil.hash(password));
        return "Faculty added. Login username: " + username;
    }

    private Object updateFaculty(ApiRequest req) throws SQLException {
        Faculty old = facultyDAO.findById(req.integer("id"))
                .orElseThrow(() -> ApiException.notFound("Faculty not found."));
        facultyDAO.update(new Faculty(old.facultyId(), old.userId(), req.str("name"),
                Validators.email(req.str("email")), req.integer("deptId"), null, req.str("designation")));
        return "Faculty updated.";
    }

    private Object deleteFaculty(ApiRequest req) throws SQLException {
        Faculty f = facultyDAO.findById(req.integer("id"))
                .orElseThrow(() -> ApiException.notFound("Faculty not found."));
        userDAO.delete(f.userId()); // their courses become "unassigned"
        return "Faculty deleted.";
    }

    // ---------------- courses ----------------

    private Object addCourse(ApiRequest req) throws SQLException {
        courseDAO.add(req.str("courseCode").toUpperCase(), req.str("courseName"),
                req.integer("credits", 1, 6), req.integer("deptId"), req.optInteger("facultyId"));
        return "Course added.";
    }

    private Object updateCourse(ApiRequest req) throws SQLException {
        int id = req.integer("id");
        courseDAO.update(id, req.str("courseName"), req.integer("credits", 1, 6), req.integer("deptId"));
        courseDAO.assignFaculty(id, req.optInteger("facultyId")); // blank = unassigned
        return "Course updated.";
    }

    private Object deleteCourse(ApiRequest req) throws SQLException {
        courseDAO.delete(req.integer("id"));
        return "Course deleted.";
    }

    // ---------------- enrollments ----------------

    private Object enroll(ApiRequest req) throws SQLException {
        int studentId = req.integer("studentId");
        int courseId = req.integer("courseId");
        if (enrollDAO.isEnrolled(studentId, courseId)) {
            throw new ApiException(409, "Student is already enrolled in this course.");
        }
        enrollDAO.enroll(studentId, courseId);
        return "Student enrolled.";
    }

    private Object drop(ApiRequest req) throws SQLException {
        if (!enrollDAO.drop(req.integer("studentId"), req.integer("courseId"))) {
            throw ApiException.notFound("Student was not enrolled in that course.");
        }
        return "Enrollment removed.";
    }

    // ---------------- exams ----------------

    private Object scheduleExam(ApiRequest req) throws SQLException {
        examDAO.add(req.integer("courseId"), req.str("examName"), req.date("examDate"),
                req.integer("maxMarks", 1, 500));
        return "Exam scheduled.";
    }

    private Object deleteExam(ApiRequest req) throws SQLException {
        examDAO.delete(req.integer("id"));
        return "Exam deleted.";
    }

    // ---------------- fees ----------------

    private Map<String, Object> feeSummary() throws SQLException {
        BigDecimal billed = feeDAO.totalBilled();
        BigDecimal collected = feeDAO.totalCollected();
        return Dto.map("billed", billed, "collected", collected, "outstanding", billed.subtract(collected));
    }

    private Object ledger(ApiRequest req) throws SQLException {
        int studentId = req.integer("studentId");
        return Dto.map("fees", Dto.fees(feeDAO.findByStudent(studentId)),
                "payments", feeDAO.findPaymentsByStudent(studentId));
    }

    private Object addFee(ApiRequest req) throws SQLException {
        feeDAO.addFee(req.integer("studentId"), req.str("description"), req.amount("amount"), req.date("dueDate"));
        return "Fee added.";
    }

    private Object addFeeForAll(ApiRequest req) throws SQLException {
        int n = feeDAO.addFeeForAllStudents(req.str("description"), req.amount("amount"), req.date("dueDate"));
        return "Fee added for " + n + " students.";
    }

    private Object recordPayment(ApiRequest req) throws SQLException {
        feeDAO.recordPayment(req.integer("feeId"), req.amount("amount"), Validators.paymentMode(req.str("mode")));
        return "Payment recorded.";
    }

    // ---------------- user accounts ----------------

    private Object resetPassword(ApiRequest req) throws SQLException {
        User u = userDAO.findById(req.integer("userId"))
                .orElseThrow(() -> ApiException.notFound("User not found."));
        userDAO.updatePassword(u.userId(), PasswordUtil.hash(Validators.password(req.str("password"))));
        return "Password reset for " + u.username() + ".";
    }

    private Object toggleUser(ApiRequest req) throws SQLException {
        User u = userDAO.findById(req.integer("userId"))
                .orElseThrow(() -> ApiException.notFound("User not found."));
        if (u.userId() == req.user().userId()) {
            throw ApiException.badRequest("You cannot disable your own account.");
        }
        userDAO.setActive(u.userId(), !u.active());
        return u.username() + " is now " + (u.active() ? "disabled." : "enabled.");
    }
}
