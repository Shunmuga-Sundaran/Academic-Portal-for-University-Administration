package com.portal.service;

import com.portal.dao.UserDAO;
import com.portal.model.User;
import com.portal.util.PasswordUtil;
import java.sql.SQLException;
import java.util.Optional;

/** Login and password management. */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /** Returns the user only if the account exists, is active and the password matches. */
    public Optional<User> login(String username, String password) throws SQLException {
        Optional<User> found = userDAO.findByUsername(username);
        if (found.isEmpty() || !found.get().active()) {
            return Optional.empty();
        }
        return PasswordUtil.verify(password, found.get().passwordHash()) ? found : Optional.empty();
    }

    public boolean changePassword(User user, String oldPassword, String newPassword) throws SQLException {
        // re-read from the DB so a second password change in the same session still works
        User current = userDAO.findByUsername(user.username()).orElse(null);
        if (current == null || !PasswordUtil.verify(oldPassword, current.passwordHash())) {
            return false;
        }
        userDAO.updatePassword(current.userId(), PasswordUtil.hash(newPassword));
        return true;
    }
}
