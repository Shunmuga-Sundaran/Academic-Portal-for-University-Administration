package com.portal.model;

public record Student(int studentId, int userId, String rollNo, String name, String email,
                      String phone, int deptId, String deptName, int semester) { }
