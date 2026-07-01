import { test, expect } from '@playwright/test';

test('has title and file input', async ({ page }) => {
  await page.goto('http://localhost:3000');
  await expect(page).toHaveTitle(/OpenLucky/);
  const fileInput = page.locator('#fileInput');
  await expect(fileInput).toBeVisible();
});
