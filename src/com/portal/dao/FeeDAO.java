package com.portal.dao;

import com.portal.config.DBConnection;
import com.portal.model.Fee;
import com.portal.model.Payment;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FeeDAO extends BaseDAO<Fee> {

    // 'paid' is calculated from the payments table, so the balance can never go out of sync.
    private static final String SELECT =
            "SELECT f.fee_id, f.student_id, s.roll_no, s.name, f.description, f.amount, f.due_date, "
          + "COALESCE(SUM(p.amount_paid), 0) AS paid FROM fees f "
          + "JOIN students s ON f.student_id = s.student_id "
          + "LEFT JOIN payments p ON p.fee_id = f.fee_id ";

    private static final String GROUP =
            "GROUP BY f.fee_id, f.student_id, s.roll_no, s.name, f.description, f.amount, f.due_date ";

    @Override
    protected Fee map(ResultSet rs) throws SQLException {
        return new Fee(rs.getInt("fee_id"), rs.getInt("student_id"), rs.getString("roll_no"),
                rs.getString("name"), rs.getString("description"), rs.getBigDecimal("amount"),
                rs.getDate("due_date").toLocalDate(), rs.getBigDecimal("paid"));
    }

    public void addFee(int studentId, String description, BigDecimal amount, LocalDate dueDate) throws SQLException {
        Db.update("INSERT INTO fees (student_id, description, amount, due_date) VALUES (?, ?, ?, ?)",
                studentId, description, amount, dueDate);
    }

    /** Bills every student at once (e.g. tuition for the semester). Returns number of students billed. */
    public int addFeeForAllStudents(String description, BigDecimal amount, LocalDate dueDate) throws SQLException {
        return Db.update("INSERT INTO fees (student_id, description, amount, due_date) "
                + "SELECT student_id, ?, ?, ? FROM students", description, amount, dueDate);
    }

    public List<Fee> findByStudent(int studentId) throws SQLException {
        return query(SELECT + "WHERE f.student_id = ? " + GROUP + "ORDER BY f.due_date", studentId);
    }

    public List<Fee> findOutstanding() throws SQLException {
        return query(SELECT + GROUP + "HAVING f.amount > COALESCE(SUM(p.amount_paid), 0) "
                + "ORDER BY f.due_date, s.roll_no");
    }

    /**
     * Records a payment inside a transaction. The fee row is locked (FOR UPDATE) while the
     * balance is checked, so two simultaneous payments cannot overpay the same fee.
     */
    public void recordPayment(int feeId, BigDecimal amount, String mode) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal total;
                try (PreparedStatement ps = Db.prepare(con, "SELECT amount FROM fees WHERE fee_id = ? FOR UPDATE", feeId);
                     ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Fee not found.");
                    }
                    total = rs.getBigDecimal(1);
                }
                BigDecimal paid;
                try (PreparedStatement ps = Db.prepare(con,
                        "SELECT COALESCE(SUM(amount_paid), 0) FROM payments WHERE fee_id = ?", feeId);
                     ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    paid = rs.getBigDecimal(1);
                }
                BigDecimal balance = total.subtract(paid);
                if (amount.compareTo(balance) > 0) {
                    throw new IllegalArgumentException("Payment exceeds the outstanding balance of " + balance + ".");
                }
                try (PreparedStatement ps = Db.prepare(con,
                        "INSERT INTO payments (fee_id, amount_paid, payment_mode) VALUES (?, ?, ?)",
                        feeId, amount, mode)) {
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public List<Payment> findPaymentsByStudent(int studentId) throws SQLException {
        String sql = "SELECT p.payment_id, p.fee_id, f.description, p.amount_paid, p.paid_on, p.payment_mode "
                   + "FROM payments p JOIN fees f ON p.fee_id = f.fee_id "
                   + "WHERE f.student_id = ? ORDER BY p.paid_on DESC, p.payment_id DESC";
        List<Payment> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = Db.prepare(con, sql, studentId);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Payment(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getBigDecimal(4),
                        rs.getTimestamp(5).toLocalDateTime(), rs.getString(6)));
            }
        }
        return out;
    }

    public BigDecimal totalBilled() throws SQLException {
        return scalar("SELECT COALESCE(SUM(amount), 0) FROM fees");
    }

    public BigDecimal totalCollected() throws SQLException {
        return scalar("SELECT COALESCE(SUM(amount_paid), 0) FROM payments");
    }

    private BigDecimal scalar(String sql) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        }
    }
}
