package com.orasaka.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 3 — Playwright Java integration test that drives a headless browser
 * against the live gateway to validate the actuator health endpoint returns
 * a well-formed JSON response with HTTP 200 and status "UP".
 *
 * <p>Executed by maven-failsafe-plugin inside the {@code e2e-tests} profile.
 */
class GatewayHealthIT {

    private static final String GATEWAY_URL =
            System.getProperty("gateway.target.url");

    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    static void launchBrowser() {
        if (GATEWAY_URL == null || GATEWAY_URL.isBlank()) {
            throw new IllegalStateException("Target Gateway URL is missing. Check your root .env file.");
        }
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
        );
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @DisplayName("GET /actuator/health returns 200 UP via headless Chromium")
    void actuatorHealthReturnsUp() {
        Page page = browser.newPage();
        Response response = page.navigate(GATEWAY_URL + "/actuator/health");

        assertNotNull(response, "Response must not be null");
        assertEquals(200, response.status(), "Health endpoint must return 200");

        String body = page.innerText("body");
        assertTrue(body.contains("UP"), "Health status must contain 'UP'");

        page.close();
    }
}
