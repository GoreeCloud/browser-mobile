package com.goreecloud.browsermobile.core

/**
 * Deliberately device-local page controls. Find queries are transient, and
 * text zoom belongs to one tab rather than a host, cookie jar or preference.
 */
object PageToolsPolicy {
    const val MIN_TEXT_ZOOM = 80
    const val DEFAULT_TEXT_ZOOM = 100
    const val MAX_TEXT_ZOOM = 200
    private const val ZOOM_STEP = 20
    private const val MAX_FIND_CODE_UNITS = 160

    /** Clamp even unexpected values before applying them to WebView settings. */
    fun stepTextZoom(current: Int, direction: Int): Int {
        val step = direction.coerceIn(-1, 1)
        return (current.toLong() + step * ZOOM_STEP)
            .coerceIn(MIN_TEXT_ZOOM.toLong(), MAX_TEXT_ZOOM.toLong())
            .toInt()
    }

    /**
     * Bound potentially huge pasted strings and avoid splitting a UTF-16 pair
     * at the limit; never modify the text inside a normal-sized query.
     */
    fun limitFindQuery(text: String): String {
        if (text.length <= MAX_FIND_CODE_UNITS) return text
        val end = if (
            Character.isHighSurrogate(text[MAX_FIND_CODE_UNITS - 1]) &&
            Character.isLowSurrogate(text[MAX_FIND_CODE_UNITS])
        ) MAX_FIND_CODE_UNITS - 1 else MAX_FIND_CODE_UNITS
        return text.substring(0, end)
    }
}
