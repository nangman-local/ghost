import process from 'node:process';
import win32 from './win32.js';
import darwin from './darwin.js';

const platforms = { win32, darwin };

export const platform = platforms[process.platform];

if (!platform) {
  throw new Error(`지원하지 않는 OS입니다: ${process.platform} (Windows, macOS만 지원)`);
}
