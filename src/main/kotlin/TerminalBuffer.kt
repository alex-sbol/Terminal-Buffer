package org.example

class TerminalBuffer(
    private val width: Int,
    private val height: Int,
    scrollbackMaxSize: Int
            ) {
    var screen = Screen(width, height)
    var scrollback = Scrollback(scrollbackMaxSize)

    private var cursorX: Int = 0
    private var cursorY: Int = 0

    private var currentStyle: TextStyle = TextStyle()

    fun getWidth(): Int = width
    fun getHeight(): Int = height
    fun getScrollbackLineCount(): Int = scrollback.size()


}

