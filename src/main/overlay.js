import { BrowserWindow, ipcMain, screen } from 'electron';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { platform } from './platform/index.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const { alwaysOnTopLevel, visibleOnFullScreen, reassertTopMs } = platform.overlay;

/** 포커스를 뺏지 않고 오버레이를 topmost 그룹의 맨 위로 다시 올린다. */
export function bringOverlayToTop(win) {
  if (!win || win.isDestroyed() || !win.isVisible()) return;
  win.setAlwaysOnTop(true, alwaysOnTopLevel);
  win.moveTop();
}

/**
 * 주 모니터 작업 영역 전체를 덮는 투명·항상 위 창.
 * 기본은 클릭 통과(setIgnoreMouseEvents true + forward)이고,
 * 렌더러가 커서가 캐릭터 위에 있다고 알려줄 때만 마우스 이벤트를 받는다.
 */
export function createOverlayWindow() {
  const { workArea } = screen.getPrimaryDisplay();

  const win = new BrowserWindow({
    x: workArea.x,
    y: workArea.y,
    width: workArea.width,
    height: workArea.height,
    transparent: true,
    backgroundColor: '#00000000',
    frame: false,
    resizable: false,
    movable: false,
    minimizable: false,
    maximizable: false,
    fullscreenable: false,
    hasShadow: false,
    skipTaskbar: true,
    alwaysOnTop: true,
    focusable: false, // 캐릭터가 떠 있어도 사용자의 입력 포커스를 뺏지 않는다
    show: false,
    webPreferences: {
      preload: path.join(__dirname, '../preload/preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });

  win.setAlwaysOnTop(true, alwaysOnTopLevel);
  win.setVisibleOnAllWorkspaces(true, { visibleOnFullScreen });
  win.setIgnoreMouseEvents(true, { forward: true });

  let interactive = false;
  const onSetInteractive = (event, value) => {
    if (event.sender !== win.webContents) return;
    const next = Boolean(value);
    if (next === interactive) return;
    interactive = next;
    if (interactive) {
      win.setIgnoreMouseEvents(false);
    } else {
      win.setIgnoreMouseEvents(true, { forward: true });
    }
  };
  ipcMain.on('overlay:set-interactive', onSetInteractive);

  const fitToWorkArea = () => {
    if (win.isDestroyed()) return;
    win.setBounds(screen.getPrimaryDisplay().workArea);
  };
  screen.on('display-metrics-changed', fitToWorkArea);
  screen.on('display-added', fitToWorkArea);
  screen.on('display-removed', fitToWorkArea);

  win.on('always-on-top-changed', (_event, isOnTop) => {
    if (!isOnTop) bringOverlayToTop(win);
  });
  const topmostTimer = reassertTopMs ? setInterval(() => bringOverlayToTop(win), reassertTopMs) : null;

  win.on('closed', () => {
    if (topmostTimer) clearInterval(topmostTimer);
    ipcMain.removeListener('overlay:set-interactive', onSetInteractive);
    screen.removeListener('display-metrics-changed', fitToWorkArea);
    screen.removeListener('display-added', fitToWorkArea);
    screen.removeListener('display-removed', fitToWorkArea);
  });

  win.loadFile(path.join(__dirname, '../renderer/overlay.html'));
  win.once('ready-to-show', () => win.showInactive());

  return win;
}
