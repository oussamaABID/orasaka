package com.orasaka.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier 3 — Full UI E2E: Chat Session Simulation
 *
 * <p>Launches headless Chromium via Playwright Java and exercises the complete
 * user journey: navigate to the Next.js frontend, interact with ChatInputBar,
 * submit a complex prompt, and assert that streaming tokens and reasoning
 * blocks render correctly.</p>
 *
 * <p>Requires the Next.js dev server on {@code localhost:3000} and the gateway
 * on the port configured via {@code .env}.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatSessionIT {

    private static final String UI_BASE_URL =
            System.getProperty("ui.base.url");

    private static final String TEST_PROMPT =
            "Explain the theory of relativity in 3 sentences.";

    private static Playwright playwright;
    private static Browser browser;
    private static BrowserContext context;
    private static Page page;

    @BeforeAll
    static void launchBrowser() {
        if (UI_BASE_URL == null || UI_BASE_URL.isBlank()) {
            throw new IllegalStateException("Target UI URL is missing. Check your root .env file.");
        }
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
        );
        context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(1440, 900)
                        .setLocale("en-US")
        );
        page = context.newPage();
    }

    @AfterAll
    static void closeBrowser() {
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @Test
    @Order(1)
    @DisplayName("Navigate to UI and verify ChatInputBar renders")
    void shouldRenderChatInputBar() {
        page.navigate(UI_BASE_URL);

        // Wait for the chat input textarea to appear (max 30s for cold Next.js start)
        Locator chatInput = page.locator("[data-testid='chat-input']");
        chatInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(30_000));

        assertTrue(chatInput.isVisible(), "ChatInputBar textarea should be visible");
        assertTrue(chatInput.isEnabled(), "ChatInputBar textarea should be enabled");
    }

    @Test
    @Order(2)
    @DisplayName("Fill complex prompt and verify input state")
    void shouldFillPromptText() {
        Locator chatInput = page.locator("[data-testid='chat-input']");
        chatInput.fill(TEST_PROMPT);

        String value = chatInput.inputValue();
        assertTrue(value.contains("relativity"),
                "Input should contain the prompt text");

        // Submit button should become enabled
        Locator submitButton = page.locator("[data-testid='chat-submit']");
        assertTrue(submitButton.isEnabled(),
                "Submit button should be enabled when input has text");
    }

    @Test
    @Order(3)
    @DisplayName("Submit prompt and verify streaming response begins")
    void shouldSubmitAndReceiveStreamingResponse() {
        Locator submitButton = page.locator("[data-testid='chat-submit']");
        submitButton.click();

        // After submission, the input should be locked (agent is busy)
        Locator chatInput = page.locator("[data-testid='chat-input']");
        page.waitForCondition(() -> {
            String disabled = chatInput.getAttribute("disabled");
            return disabled != null;
        }, new Page.WaitForConditionOptions().setTimeout(10_000));

        // Wait for at least one message bubble / assistant response to appear
        // The response area typically uses role="assistant" or a message container
        Locator assistantMessage = page.locator(
                "[data-role='assistant'], .message-bubble, [class*='assistant']"
        ).first();

        assistantMessage.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(60_000));

        assertTrue(assistantMessage.isVisible(),
                "At least one assistant message should appear after submission");
    }

    @Test
    @Order(4)
    @DisplayName("Verify streaming tokens contain meaningful content")
    void shouldRenderStreamingTokens() {
        // Wait for the response to accumulate some text (streaming may take a few seconds)
        page.waitForTimeout(5_000);

        Locator assistantMessage = page.locator(
                "[data-role='assistant'], .message-bubble, [class*='assistant']"
        ).first();

        String responseText = assistantMessage.textContent();
        assertNotNull(responseText, "Response text should not be null");
        assertFalse(responseText.isBlank(), "Response should contain rendered tokens");

        // The response should contain actual language (not just loading indicators)
        assertTrue(responseText.length() > 20,
                "Response should contain meaningful streaming content (got: " + responseText.length() + " chars)");
    }

    @Test
    @Order(5)
    @DisplayName("Verify input unlocks after response completes")
    void shouldUnlockInputAfterCompletion() {
        // Wait for streaming to complete — input should re-enable
        Locator chatInput = page.locator("[data-testid='chat-input']");

        page.waitForCondition(() -> {
            String disabled = chatInput.getAttribute("disabled");
            return disabled == null;
        }, new Page.WaitForConditionOptions().setTimeout(120_000));

        assertTrue(chatInput.isEnabled(),
                "ChatInputBar should unlock after agent completes response");
    }
}
