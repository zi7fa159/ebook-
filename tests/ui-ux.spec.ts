import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

test('wavelet sliders are present', async ({ page }) => {
  await page.goto('http://localhost:3000');
  await expect(page.locator('#waveletLayer1')).toBeVisible();
  await expect(page.locator('#waveletLayer2')).toBeVisible();
});
