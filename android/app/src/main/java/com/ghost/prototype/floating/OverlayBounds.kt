package com.ghost.prototype.floating

/** Pixel bounds for the window's top-left corner, excluding system bars and cutouts. */
data class OverlayBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun clamp(position: OverlayPosition, width: Int, height: Int): OverlayPosition =
        OverlayPosition(
            position.x.coerceIn(left, maxOf(left, right - width)),
            position.y.coerceIn(top, maxOf(top, bottom - height)),
        )

    fun initial(width: Int, height: Int, margin: Int): OverlayPosition = clamp(
        OverlayPosition(right - width - margin, top + (bottom - top - height) / 2),
        width,
        height,
    )

    /**
     * 화면 정중앙(#63). 유령이 어디에 드래그돼 있었든, 2분 협상은 항상 여기서 뜬다.
     *
     * 유령 위치를 그대로 키우면 화면 아무 데서나 커져서 하필 중요한 내용을 가릴 수 있다
     * (예측 불가능). 그 대신 항상 같은 안전한 자리에서 뜨게 해, **어디서 뜰지는 예측
     * 가능**하게 하고 사용자가 필요하면 드래그로 치울 수 있게 한다.
     */
    fun centered(width: Int, height: Int): OverlayPosition = clamp(
        OverlayPosition(left + (right - left - width) / 2, top + (bottom - top - height) / 2),
        width,
        height,
    )
}
