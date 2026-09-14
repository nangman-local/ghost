export default {
  name: 'win32',
  verified: true,

  activeWindowOptions: undefined,

  overlay: {
    alwaysOnTopLevel: 'screen-saver',
    visibleOnFullScreen: false,
    // topmost 창끼리는 "나중에 올라온 창"이 위에 온다. 캡처 도구, PIP 영상, 메신저 알림에 가려진 뒤
    // 스스로 복구되지 않으므로 주기적으로 맨 위를 다시 확보한다.
    reassertTopMs: 1000,
  },
};
