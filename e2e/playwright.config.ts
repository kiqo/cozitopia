import { defineConfig } from "@playwright/test";

export default defineConfig({
    testDir: ".",
    testMatch: "**/*.spec.ts",
    timeout: 120_000,
    use: {
        baseURL: "http://localhost:8081/",
        trace: "off",
    },
    projects: [
        { name: "phone", use: { viewport: { width: 390, height: 844 } } },
        { name: "desktop", use: { viewport: { width: 1280, height: 800 } } },
    ],
});
