package com.portal.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Fee(int feeId, int studentId, String rollNo, String studentName, String description,
                  BigDecimal amount, LocalDate dueDate, BigDecimal paid) {

    public BigDecimal balance() {
        return amount.subtract(paid);
    }

    public String status() {
        if (balance().signum() <= 0) {
            return "PAID";
        }
        if (dueDate.isBefore(LocalDate.now())) {
            return "OVERDUE";
        }
        return paid.signum() > 0 ? "PARTIAL" : "PENDING";
    }
}
