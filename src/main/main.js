import { app, globalShortcut, ipcMain } from 'electron';
import { bringOverlayToTop, createOverlayWindow } from './overlay.js';
import { watchActiveWindow } from './activeWindow.js';
import { platform } from './platform/index.js';

const QUIT_SHORTCUT = 'CommandOrControl+Shift+Q';

if (!app.requestSingleInstanceLock()) {
  app.quit();
}

let overlay = null;
let stopWatching = null;

app.whenReady().then(() => {
  overlay = createOverlayWindow();

  stopWatching = watchActiveWindow({
    onChange: (info) => {
      console.log(`[active] ${info.appName} | ${info.title}`);
      if (overlay && !overlay.isDestroyed()) {
        overlay.webContents.send('active-window:changed', info);
        bringOverlayToTop(overlay);
      }
    },
  });

  // 오버레이는 포커스를 받지 않으므로 종료 수단을 따로 둔다 (트레이는 이후 단계)
  globalShortcut.register(QUIT_SHORTCUT, () => app.quit());
  ipcMain.on('app:quit', () => app.quit());

  console.log(`[ghost] 스파이크 실행 중 (${platform.name}). 종료: ${QUIT_SHORTCUT} 또는 캐릭터 우클릭`);
  if (!platform.verified) {
    console.warn(`[ghost] ${platform.name}은 아직 실기기 검증 전입니다. src/main/platform/${platform.name}.js 참고`);
  }
});

app.on('will-quit', () => {
  stopWatching?.();
  globalShortcut.unregisterAll();
});

app.on('window-all-closed', () => app.quit());
