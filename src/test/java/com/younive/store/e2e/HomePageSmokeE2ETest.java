package com.younive.store.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A single browser-based smoke test. This is the lone test that drives a real browser; everything
 * else uses APIRequestContext.
 *
 * <p>The only HTML view is the Thymeleaf index at "/", which is permitAll and renders the
 * "Hello Younive" greeting for anonymous visitors.
 *
 * <p>Requires the Playwright browsers to be installed:
 * <pre>
 *   mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps"
 * </pre>
 * Headless by default; set -Dheaded=true to watch it run.
 */
@Tag("e2e")
class HomePageSmokeE2ETest {

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void launch() {
        playwright = Playwright.create();
        boolean headed = Boolean.getBoolean("headed");
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(!headed));
    }

    @AfterAll
    static void shutdown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @DisplayName("the home page renders the greeting for anonymous visitors")
    void shouldRenderHomePage() {
        try (var context = browser.newContext()) {
            Page page = context.newPage();
            Response response = page.navigate(E2EConfig.baseUrl() + "/");
            assertNotNull(response, "navigation should return a response");
            assertEquals(200, response.status(), "home page is permitAll");
            assertTrue(page.locator("h1").textContent().contains("Younive"),
                    "greeting should contain the store name");
        }
    }
}
