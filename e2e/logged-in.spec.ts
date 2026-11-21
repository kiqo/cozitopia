import { expect, test } from "@playwright/test";

const SCREENSHOT_DIR = "screenshots";
const SCREEN_NAMES = ["homes", "members", "checklist", "calendar", "profile"];

test.use({ storageState: "auth.json" });

test("logged-in screens render", async ({ page }, testInfo) => {
    const project = testInfo.project.name;
    const consoleErrors: string[] = [];
    page.on("pageerror", (error) => consoleErrors.push(`pageerror: ${error.message}`));

    await page.goto("/");
    await expect(page.locator("canvas").first()).toBeVisible({ timeout: 90_000 });
    await page.waitForTimeout(10_000);
    await page.screenshot({ path: `${SCREENSHOT_DIR}/${project}-homes.png`, fullPage: true });

    const viewport = page.viewportSize();
    if (!viewport) throw new Error("viewport size unavailable");
    const navY = viewport.height - 40;
    for (const [index, name] of SCREEN_NAMES.slice(1).entries()) {
        const x = (viewport.width * (index + 1.5)) / SCREEN_NAMES.length;
        await page.mouse.click(x, navY);
        await page.waitForTimeout(6000);
        await page.screenshot({ path: `${SCREENSHOT_DIR}/${project}-${name}.png`, fullPage: true });
    }
    console.log(`${project} console errors: ${JSON.stringify(consoleErrors)}`);
});
