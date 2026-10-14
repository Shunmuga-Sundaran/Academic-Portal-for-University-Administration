package com.portal.model;

public record User(int userId, String username, String passwordHash, Role role, boolean active) { }
