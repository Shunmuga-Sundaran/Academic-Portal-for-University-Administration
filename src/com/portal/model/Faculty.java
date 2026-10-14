package com.portal.model;

public record Faculty(int facultyId, int userId, String name, String email,
                      int deptId, String deptName, String designation) { }
