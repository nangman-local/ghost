// 미검증 초안: 맥 스파이크에서 실기기로 확인하기 전까지 값이 맞는지 보장하지 않는다.
export default {
  name: 'darwin',
  verified: false,

  // 화면 기록 권한이 없으면 title이 빈 문자열, 손쉬운 사용 권한이 없으면 url이 오지 않는다.
  // 권한 안내 온보딩을 만들기 전까지는 get-windows가 권한 프롬프트를 띄우게 둔다.
  activeWindowOptions: { accessibilityPermission: true, screenRecordingPermission: true },

  overlay: {
    alwaysOnTopLevel: 'screen-saver',
    // 전체화면 앱은 별도 Space에 뜨므로 거기서도 캐릭터가 보이게 한다.
    visibleOnFullScreen: true,
    // 창 레벨 기반이라 Windows 같은 주기적 재확보가 필요한지 확인 전. 필요하면 값을 넣는다.
    reassertTopMs: null,
  },
};
