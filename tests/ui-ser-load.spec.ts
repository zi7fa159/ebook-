import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

test('loading a mock SER file', async ({ page }) => {
  await page.goto('http://localhost:3000');

  // Create a mock SER file buffer
  const buffer = Buffer.alloc(178 + 640 * 480);
  buffer.write('LUCAM-RECORDER', 0, 14, 'ascii');
  buffer.writeUInt32LE(0, 14); // luID
  buffer.writeUInt32LE(0, 18); // colorID (Mono)
  buffer.writeUInt32LE(0, 22); // littleEndian
  buffer.writeUInt32LE(640, 26); // width
  buffer.writeUInt32LE(480, 30); // height
  buffer.writeUInt32LE(8, 34); // bitDepth
  buffer.writeUInt32LE(1, 38); // frameCount

  // Write some "image" data
  for (let i = 0; i < 640 * 480; i++) {
      buffer.writeUInt8(Math.floor(Math.random() * 256), 178 + i);
  }

  const filePath = path.join(process.cwd(), 'mock.ser');
  fs.writeFileSync(filePath, buffer);

  await page.setInputFiles('#fileInput', filePath);

  await expect(page.locator('#fileInfo')).toContainText('640x480');
  await expect(page.locator('#fileInfo')).toContainText('Frames: 1');

  // Verify canvas
  const canvasVisible = await page.locator('#mainCanvas').isVisible();
  expect(canvasVisible).toBe(true);

  // Clean up
  fs.unlinkSync(filePath);
});
