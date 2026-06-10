package com.younive.store.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Helper for the admin-only flows. The application has no seeded admin and registration always
 * assigns role USER, so the only way to obtain an admin in an E2E context is to register a user
 * through the public API and then flip {@code users.role} to {@code 'ADMIN'} directly in MySQL.
 *
 * <p>The connection uses the {@code e2e.mysql.*} system properties (see {@link E2EConfig}).
 * mysql-connector-j is already on the (runtime) classpath of the project.
 */
final class AdminSupport {

    private AdminSupport() {
    }

    /** Promotes the user with the given email to ADMIN. Returns true if a row was updated. */
    static boolean promoteToAdmin(String email) {
        String sql = "UPDATE users SET role = 'ADMIN' WHERE email = ?";
        try (Connection conn = DriverManager.getConnection(
                E2EConfig.mysqlUrl(), E2EConfig.mysqlUser(), E2EConfig.mysqlPassword());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not promote '" + email + "' to ADMIN via JDBC (" + E2EConfig.mysqlUrl()
                            + "). Ensure MySQL is reachable and -De2e.mysql.* properties are correct.", e);
        }
    }
}
