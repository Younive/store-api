package com.younive.store.e2e;

/**
 * Central configuration for the E2E suite, read from system properties so the
 * suite is CI-friendly and never hardcodes environment-specific values.
 *
 * <p>Override on the command line, e.g.:
 * <pre>
 *   mvn test -Pe2e -DbaseUrl=http://localhost:8080 \
 *            -De2e.mysql.url=jdbc:mysql://localhost:3306/store_api \
 *            -De2e.mysql.user=root -De2e.mysql.password=secret
 * </pre>
 */
final class E2EConfig {

    private E2EConfig() {
    }

    /** Base URL of the running Spring Boot app. */
    static String baseUrl() {
        return trimTrailingSlash(System.getProperty("baseUrl", "http://localhost:8080"));
    }

    /** JDBC URL used only to promote a freshly-registered user to ADMIN (no seeded admin exists). */
    static String mysqlUrl() {
        return System.getProperty("e2e.mysql.url", "jdbc:mysql://localhost:3306/store_api");
    }

    static String mysqlUser() {
        // Fall back to the same env var the app uses for local convenience.
        return System.getProperty("e2e.mysql.user", System.getenv().getOrDefault("MYSQL_USER", "root"));
    }

    static String mysqlPassword() {
        String prop = System.getProperty("e2e.mysql.password");
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return System.getenv().getOrDefault("MYSQL_PASSWORD", "");
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
