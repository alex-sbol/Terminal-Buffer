package org.example

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TerminalBufferTest {

    @Test
    fun `initial dimensions and empty content are correct`() {
        val buffer = TerminalBuffer(5, 3, 10)

        assertEquals(5, buffer.getWidth())
        assertEquals(3, buffer.getHeight())
        assertEquals(0, buffer.getScrollbackLineCount())
        assertEquals(TextStyle(), buffer.getCurrentStyle())
        assertEquals(Pair(0, 0), buffer.getCursorPosition())
        assertEquals("\n\n", buffer.getScreenContentAsString())
    }

    @Test
    fun `constructor validation rejects invalid dimensions`() {
        assertThrows(IllegalArgumentException::class.java) { TerminalBuffer(0, 3, 1) }
        assertThrows(IllegalArgumentException::class.java) { TerminalBuffer(3, 0, 1) }
        assertThrows(IllegalArgumentException::class.java) { TerminalBuffer(3, 3, -1) }
    }

    @Test
    fun `set attributes affects further writes`() {
        val buffer = TerminalBuffer(5, 2, 10)
        buffer.setAttributes(
            foreground = Colors.ELEVEN,
            background = Colors.THIRTEEN,
            bold = true,
            italic = true,
            underline = true
        )

        buffer.writeText("A")
        val cell = buffer.getCellAt(0, 0)
        val style = buffer.getAttributesAt(0, 0)

        assertEquals('A', cell.char)
        assertEquals(
            TextStyle(
                foreground = Colors.ELEVEN,
                background = Colors.THIRTEEN,
                bold = true,
                italic = true,
                underline = true
            ),
            style
        )
    }

    @Test
    fun `cursor set and move stays within bounds`() {
        val buffer = TerminalBuffer(5, 3, 10)

        buffer.setCursorPosition(99, 99)
        assertEquals(Pair(4, 2), buffer.getCursorPosition())

        buffer.moveCursorRight(10)
        assertEquals(Pair(4, 2), buffer.getCursorPosition())

        buffer.moveCursorDown(10)
        assertEquals(Pair(4, 2), buffer.getCursorPosition())

        buffer.moveCursorLeft(10)
        assertEquals(Pair(0, 2), buffer.getCursorPosition())

        buffer.moveCursorUp(10)
        assertEquals(Pair(0, 0), buffer.getCursorPosition())
    }

    @Test
    fun `write text overwrites current line and moves cursor`() {
        val buffer = TerminalBuffer(5, 2, 10)

        buffer.writeText("abc")

        assertEquals("abc\n", buffer.getScreenContentAsString())
        assertEquals(Pair(3, 0), buffer.getCursorPosition())
    }

    @Test
    fun `write text overrides existing content from cursor`() {
        val buffer = TerminalBuffer(5, 2, 10)

        buffer.writeText("abcde")
        buffer.setCursorPosition(1, 0)
        buffer.writeText("XY")

        assertEquals("aXYde\n", buffer.getScreenContentAsString())
    }

    @Test
    fun `write text at last column stays within line`() {
        val buffer = TerminalBuffer(4, 2, 10)

        buffer.setCursorPosition(3, 0)
        buffer.writeText("ZZ")

        assertEquals("   Z\n", buffer.getScreenContentAsString())
        assertEquals(Pair(3, 0), buffer.getCursorPosition())
    }

    @Test
    fun `insert text shifts content right on the same line`() {
        val buffer = TerminalBuffer(5, 2, 10)

        buffer.writeText("abc")
        buffer.setCursorPosition(1, 0)
        buffer.insertText("Z")

        assertEquals("aZbc\n", buffer.getScreenContentAsString())
        assertEquals('Z', buffer.getCharacterAt(0, 1))
    }

    @Test
    fun `insert text wraps overflow into next line`() {
        val buffer = TerminalBuffer(4, 2, 10)

        buffer.writeText("abcd")
        buffer.setCursorPosition(2, 0)
        buffer.insertText("XY")

        assertEquals("abXY\ncd", buffer.getScreenContentAsString())
    }

    @Test
    fun `insert text at bottom right scrolls when wrapping`() {
        val buffer = TerminalBuffer(3, 2, 10)

        buffer.writeText("abc")
        buffer.setCursorPosition(0, 1)
        buffer.writeText("de")
        buffer.setCursorPosition(2, 1)
        buffer.insertText("Z")

        assertEquals(1, buffer.getScrollbackLineCount())
        assertEquals("deZ\n", buffer.getScreenContentAsString())
        assertEquals("abc\ndeZ\n", buffer.getAllContentAsString())
    }

    @Test
    fun `fill line uses current attributes`() {
        val buffer = TerminalBuffer(4, 2, 10)
        buffer.setAttributes(foreground = Colors.THREE, bold = true)

        buffer.fillLine(1, '*')

        assertEquals("\n****", buffer.getScreenContentAsString())
        val style = buffer.getAttributesAt(1, 2)
        assertEquals('*', buffer.getCharacterAt(1, 2))
        assertEquals(Colors.THREE, style.foreground)
        assertTrue(style.bold)
    }

    @Test
    fun `fill line with null produces blanks`() {
        val buffer = TerminalBuffer(3, 1, 1)
        buffer.writeText("abc")

        buffer.fillLine(0, null)

        assertEquals("", buffer.getLineAsString(0))
        assertNull(buffer.getCharacterAt(0, 0))
    }

    @Test
    fun `insert empty line at bottom scrolls top line into scrollback`() {
        val buffer = TerminalBuffer(4, 2, 10)

        buffer.writeText("top")
        buffer.setCursorPosition(0, 1)
        buffer.writeText("bot")

        buffer.insertEmptyLineAtBottom()

        assertEquals(1, buffer.getScrollbackLineCount())
        assertEquals("bot\n", buffer.getScreenContentAsString())
        assertEquals("top\nbot\n", buffer.getAllContentAsString())
    }

    @Test
    fun `clear screen keeps scrollback`() {
        val buffer = TerminalBuffer(4, 2, 10)

        buffer.writeText("top")
        buffer.setCursorPosition(0, 1)
        buffer.writeText("bot")
        buffer.insertEmptyLineAtBottom()

        buffer.clearScreen()

        assertEquals(1, buffer.getScrollbackLineCount())
        assertEquals("\n", buffer.getScreenContentAsString())
        assertEquals("top\n\n", buffer.getAllContentAsString())
        assertEquals(Pair(0, 0), buffer.getCursorPosition())
    }

    @Test
    fun `clear screen and scrollback resets everything`() {
        val buffer = TerminalBuffer(4, 2, 10)

        buffer.writeText("top")
        buffer.insertEmptyLineAtBottom()
        buffer.clearScreenAndScrollback()

        assertEquals(0, buffer.getScrollbackLineCount())
        assertEquals("\n", buffer.getScreenContentAsString())
        assertEquals("\n", buffer.getAllContentAsString())
        assertEquals(Pair(0, 0), buffer.getCursorPosition())
    }

    @Test
    fun `can read chars and lines from screen and scrollback`() {
        val buffer = TerminalBuffer(4, 2, 10)

        buffer.writeText("1111")
        buffer.setCursorPosition(0, 1)
        buffer.writeText("2222")
        buffer.insertEmptyLineAtBottom()

        assertEquals('1', buffer.getCharacterAt(0, 0))
        assertEquals('2', buffer.getCharacterAt(1, 0))
        assertNull(buffer.getCharacterAt(2, 0))
        assertEquals("1111", buffer.getLineAsString(0))
        assertEquals("2222", buffer.getLineAsString(1))
        assertEquals("", buffer.getLineAsString(2))
    }

    @Test
    fun `scrollback is capped at maximum size`() {
        val buffer = TerminalBuffer(3, 1, 2)

        buffer.writeText("111")
        buffer.insertEmptyLineAtBottom()
        buffer.setCursorPosition(0, 0)
        buffer.writeText("222")
        buffer.insertEmptyLineAtBottom()
        buffer.setCursorPosition(0, 0)
        buffer.writeText("333")
        buffer.insertEmptyLineAtBottom()
        buffer.setCursorPosition(0, 0)

        assertEquals(2, buffer.getScrollbackLineCount())
        assertEquals("222\n333\n", buffer.getAllContentAsString())
    }

    @Test
    fun `zero scrollback discards scrolled lines`() {
        val buffer = TerminalBuffer(3, 1, 0)

        buffer.writeText("111")
        buffer.insertEmptyLineAtBottom()

        assertEquals(0, buffer.getScrollbackLineCount())
        assertEquals("", buffer.getAllContentAsString())
    }

    @Test
    fun `out of bounds access throws`() {
        val buffer = TerminalBuffer(3, 2, 2)

        assertThrows(IllegalArgumentException::class.java) {
            buffer.getCharacterAt(99, 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            buffer.getAttributesAt(0, 99)
        }
    }

    @Test
    fun `default cells are empty and unstyled`() {
        val buffer = TerminalBuffer(2, 1, 1)
        val cell = buffer.getCellAt(0, 0)
        val style = buffer.getAttributesAt(0, 0)

        assertNull(cell.char)
        assertEquals(TextStyle(), style)
        assertFalse(style.bold)
        assertFalse(style.italic)
        assertFalse(style.underline)
    }

    @Test
    fun `debug dump includes screen and scrollback sections`() {
        val buffer = TerminalBuffer(3, 1, 2)
        buffer.writeText("abc")
        buffer.insertEmptyLineAtBottom()

        val dump = buffer.dump()

        assertTrue(dump.contains("=== Scrollback ==="))
        assertTrue(dump.contains("=== Screen ==="))
        assertTrue(dump.contains("abc"))
    }
}
