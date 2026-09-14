// Electron 없이 get-windows만 확인하는 스크립트. 3초 뒤 활성 창을 1회 출력한다.
import { readActiveWindow } from '../src/main/activeWindow.js';

console.log('3초 안에 확인할 창을 클릭하세요...');
setTimeout(async () => {
  const info = await readActiveWindow();
  if (!info) {
    console.error('결과 없음: 네이티브 바인딩 로드 실패 가능성');
    process.exitCode = 1;
    return;
  }
  console.log(JSON.stringify(info, null, 2));
}, 3000);
