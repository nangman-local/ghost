import { activeWindow } from 'get-windows';
import { platform } from './platform/index.js';

const DEFAULT_INTERVAL_MS = 1000;

// 서버 정책상 URL 경로·쿼리스트링은 내보내지 않고 호스트만 남긴다.
function toDomain(url) {
  if (!url) return null;
  try {
    return new URL(url).hostname || null;
  } catch {
    return null;
  }
}

/**
 * 현재 활성 창을 OS와 무관한 형태로 반환한다. 활성 창이 없거나 네이티브 바인딩 로드에 실패하면 null.
 * domain은 브라우저 URL을 얻을 수 있는 OS(macOS)에서만 채워지고, Windows에서는 항상 null이다.
 */
export async function readActiveWindow() {
  const win = await activeWindow(platform.activeWindowOptions);
  if (!win) return null;
  return {
    appName: win.owner.name,
    title: win.title,
    domain: toDomain(win.url),
    path: win.owner.path,
    processId: win.owner.processId,
    at: new Date().toISOString(),
  };
}

/**
 * 활성 창을 주기적으로 조회하고, 앱/제목/도메인이 바뀌었을 때만 onChange를 호출한다.
 * GHOST 자신의 창(processId === ownPid)은 무시한다. 캐릭터를 클릭하면 GHOST가 활성 창으로 잡히기 때문.
 */
export function watchActiveWindow({ onChange, ownPid = process.pid, intervalMs = DEFAULT_INTERVAL_MS }) {
  let lastKey = null;
  let busy = false;
  let warnedEmpty = false;

  const tick = async () => {
    if (busy) return;
    busy = true;
    try {
      const info = await readActiveWindow();

      // 네이티브 바인딩 로드에 실패하면 get-windows는 에러 없이 undefined를 반환한다.
      // 잠금 화면 등 활성 창이 없는 경우에도 undefined가 올 수 있다.
      if (!info) {
        if (!warnedEmpty) {
          console.warn('[activeWindow] 결과가 비어 있습니다 (활성 창 없음 또는 네이티브 바인딩 로드 실패)');
          warnedEmpty = true;
        }
        return;
      }
      warnedEmpty = false;

      if (info.processId === ownPid) return;

      const key = `${info.processId}|${info.appName}|${info.title}|${info.domain}`;
      if (key === lastKey) return;
      lastKey = key;

      onChange(info);
    } catch (err) {
      console.error('[activeWindow] 조회 실패:', err);
    } finally {
      busy = false;
    }
  };

  tick();
  const timer = setInterval(tick, intervalMs);
  return () => clearInterval(timer);
}
