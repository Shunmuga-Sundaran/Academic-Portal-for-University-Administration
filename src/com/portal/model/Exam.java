package com.portal.model;

import java.time.LocalDate;

public record Exam(int examId, int courseId, String courseCode, String examName,
                   LocalDate examDate, int maxMarks) { }
