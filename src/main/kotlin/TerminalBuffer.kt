package org.example

class TerminalBuffer(width: Int, height: Int, size: Int) {
    var screen = Screen(width, height)
    var buffer = Scrollback(size)

    // attributes

    //cursor
}