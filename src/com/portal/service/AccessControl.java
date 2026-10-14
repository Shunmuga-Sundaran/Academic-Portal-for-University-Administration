package com.portal.service;

import com.portal.model.Role;
import com.portal.model.User;

/**
 * Role-Based Access Control (RBAC) guard. Every menu calls this before it opens, so a
 * student account can never reach the admin or faculty screens even through a code bug.
 */
public final class AccessControl {

    private AccessControl() { }

    public static void require(User user, Role... allowed) {
        for (Role r : allowed) {
            if (user.role() == r) {
                return;
            }
        }
        throw new SecurityException("Access denied: role " + user.role() + " is not allowed here.");
    }
}
