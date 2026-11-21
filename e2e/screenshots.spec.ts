import { expect, test } from "@playwright/test";

const SCREENSHOT_DIR = "screenshots";

test("sign-in screen renders", async ({ page }, testInfo) => {
    const consoleErrors: string[] = [];
    page.on("console", (message) => {
        if (message.type() === "error") consoleErrors.push(message.text());
    });
    page.on("pageerror", (error) => consoleErrors.push(`pageerror: ${error.message}`));

    await page.goto("/");
    await expect(page.locator("canvas").first()).toBeVisible({ timeout: 90_000 });
    await page.waitForTimeout(8000);

    await page.screenshot({
        path: `${SCREENSHOT_DIR}/${testInfo.project.name}-signin.png`,
        fullPage: true,
    });
    console.log(`${testInfo.project.name} console errors: ${JSON.stringify(consoleErrors)}`);
});
