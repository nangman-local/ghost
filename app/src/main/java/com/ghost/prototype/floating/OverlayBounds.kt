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
}

