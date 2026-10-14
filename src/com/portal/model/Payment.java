package com.portal.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Payment(int paymentId, int feeId, String description, BigDecimal amount,
                      LocalDateTime paidOn, String mode) { }
