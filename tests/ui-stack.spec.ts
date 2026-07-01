import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

test('stacking runs and completes', async ({ page }) => {
  await page.goto('http://localhost:3000');

  const frameCount = 4;
  const width = 100;
  const height = 100;
  const buffer = Buffer.alloc(178 + frameCount * width * height);
  buffer.write('LUCAM-RECORDER', 0, 14, 'ascii');
  buffer.writeUInt32LE(0, 14);
  buffer.writeUInt32LE(0, 18);
  buffer.writeUInt32LE(0, 22);
  buffer.writeUInt32LE(width, 26);
  buffer.writeUInt32LE(height, 30);
  buffer.writeUInt32LE(8, 34);
  buffer.writeUInt32LE(frameCount, 38);

  for (let i = 0; i < frameCount * width * height; i++) {
      buffer.writeUInt8(Math.floor(Math.random() * 256), 178 + i);
  }

  const filePath = path.join(process.cwd(), 'stack-test.ser');
  fs.writeFileSync(filePath, buffer);

  await page.setInputFiles('#fileInput', filePath);

  const stackBtn = page.locator('#stackBtn');
  await expect(stackBtn).toBeEnabled({ timeout: 15000 });
  await stackBtn.click();

  await expect(page.locator('#stats')).toContainText('Stacking complete', { timeout: 20000 });

  fs.unlinkSync(filePath);
});
