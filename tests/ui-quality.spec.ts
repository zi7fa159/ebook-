import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

test('quality analysis runs and completes', async ({ page }) => {
  await page.goto('http://localhost:3000');

  const frameCount = 5;
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

  const filePath = path.join(process.cwd(), 'quality-test.ser');
  fs.writeFileSync(filePath, buffer);

  await page.setInputFiles('#fileInput', filePath);

  await expect(page.locator('#stats')).toContainText('Quality analysis complete', { timeout: 10000 });

  fs.unlinkSync(filePath);
});
