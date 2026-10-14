package com.portal.model;

public enum Role {
    ADMIN(1), FACULTY(2), STUDENT(3);

    private final int id;

    Role(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Role fromId(int id) {
        for (Role r : values()) {
            if (r.id == id) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role id: " + id);
    }
}
