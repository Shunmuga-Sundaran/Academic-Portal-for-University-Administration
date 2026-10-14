package com.portal.model;

public record Course(int courseId, String courseCode, String courseName, int credits,
                     int deptId, String deptName, Integer facultyId, String facultyName) { }
