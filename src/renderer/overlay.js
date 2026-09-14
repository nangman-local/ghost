const character = document.getElementById('character');
const bubble = document.getElementById('bubble');
const bubbleApp = document.getElementById('bubble-app');
const bubbleTitle = document.getElementById('bubble-title');

const MARGIN = 24;
const DRAG_THRESHOLD_PX = 4;

// ---- 위치 ----
let pos = {
  x: window.innerWidth - character.offsetWidth - MARGIN,
  y: window.innerHeight - character.offsetHeight - MARGIN,
};

function clampPosition() {
  pos.x = Math.min(Math.max(0, pos.x), window.innerWidth - character.offsetWidth);
  pos.y = Math.min(Math.max(0, pos.y), window.innerHeight - character.offsetHeight);
}

function render() {
  clampPosition();
  character.style.left = `${pos.x}px`;
  character.style.top = `${pos.y}px`;

  // 말풍선은 캐릭터 왼쪽 위, 화면 밖으로 나가면 오른쪽으로
  const bw = bubble.offsetWidth;
  const bh = bubble.offsetHeight;
  let bx = pos.x - bw - 8;
  if (bx < 0) bx = pos.x + character.offsetWidth + 8;
  const by = Math.max(0, pos.y - bh + 24);
  bubble.style.left = `${bx}px`;
  bubble.style.top = `${by}px`;
}

window.addEventListener('resize', render);

// ---- 클릭 통과 제어 ----
// 창은 기본적으로 클릭 통과 상태이고, forward 옵션 덕분에 mousemove는 계속 들어온다.
// 커서가 캐릭터 위에 있을 때만 마우스 이벤트를 받도록 main에 알린다.
let interactive = false;
let dragging = null;

function setInteractive(value) {
  if (value === interactive) return;
  interactive = value;
  window.ghost.setInteractive(value);
}

document.addEventListener('mousemove', (e) => {
  if (dragging) return;
  setInteractive(character.contains(e.target));
});

document.addEventListener('mouseleave', () => {
  if (!dragging) setInteractive(false);
});

// ---- 드래그 / 클릭 ----
character.addEventListener('pointerdown', (e) => {
  if (e.button !== 0) return;
  character.setPointerCapture(e.pointerId);
  dragging = { startX: e.clientX, startY: e.clientY, originX: pos.x, originY: pos.y, moved: false };
});

character.addEventListener('pointermove', (e) => {
  if (!dragging) return;
  const dx = e.clientX - dragging.startX;
  const dy = e.clientY - dragging.startY;
  if (!dragging.moved && Math.hypot(dx, dy) < DRAG_THRESHOLD_PX) return;
  dragging.moved = true;
  character.classList.add('dragging');
  pos.x = dragging.originX + dx;
  pos.y = dragging.originY + dy;
  render();
});

function endDrag(e) {
  if (!dragging) return;
  const wasClick = !dragging.moved;
  dragging = null;
  character.classList.remove('dragging');
  if (wasClick && e.type === 'pointerup') poke();
  // 드래그가 끝난 지점이 캐릭터 밖이면 다시 클릭 통과로
  const under = document.elementFromPoint(e.clientX, e.clientY);
  setInteractive(Boolean(under && character.contains(under)));
}

character.addEventListener('pointerup', endDrag);
character.addEventListener('pointercancel', endDrag);

function poke() {
  character.classList.remove('poked');
  void character.offsetWidth; // 애니메이션 재시작
  character.classList.add('poked');
  console.log('[overlay] character clicked');
}

character.addEventListener('contextmenu', (e) => {
  e.preventDefault();
  window.ghost.quit();
});

// ---- 활성 창 표시 ----
window.ghost.onActiveWindowChanged((info) => {
  bubbleApp.textContent = info.appName || '(알 수 없는 앱)';
  bubbleTitle.textContent = info.title || '(제목 없음)';
  render();
});

render();
