package org.example

class TerminalBuffer(
    width: Int,
    height: Int,
    private var scrollbackMaxSize: Int
            ) {
    init {
        require(width > 0) { "Screen width must be positive" }
        require(height > 0) { "Screen height must be positive" }
        require(scrollbackMaxSize >= 0) { "Scrollback max size cannot be negative" }
    }

    var screen = Screen(width, height)
    var scrollback = Scrollback(scrollbackMaxSize)

    private var cursorX: Int = 0
    private var cursorY: Int = 0

    private var currentStyle: TextStyle = TextStyle()

    fun getWidth(): Int = screen.width
    fun getHeight(): Int = screen.height
    fun getScrollbackLineCount(): Int = scrollback.size()

    fun setAttributes(
        foreground: Colors = currentStyle.foreground,
        background: Colors = currentStyle.background,
        bold: Boolean = currentStyle.bold,
        italic: Boolean = currentStyle.italic,
        underline: Boolean = currentStyle.underline
    ) {
        currentStyle = TextStyle(foreground, background, bold, italic, underline)
    }

    fun getCurrentStyle(): TextStyle = currentStyle

    fun getCursorPosition(): Pair<Int, Int> = Pair(cursorX, cursorY)
    fun setCursorPosition(x: Int, y: Int) {
        cursorX = x.coerceIn(0, getWidth() - 1)
        cursorY = y.coerceIn(0, getHeight() - 1)
    }

    fun moveCursorUp(n: Int = 1) = setCursorPosition(cursorX, cursorY - n)
    fun moveCursorDown(n: Int = 1) = setCursorPosition(cursorX, cursorY + n)
    fun moveCursorLeft(n: Int = 1) = setCursorPosition(cursorX - n, cursorY)
    fun moveCursorRight(n: Int = 1) = setCursorPosition(cursorX + n, cursorY)

    fun writeText(text: String) {
        for (ch in text) {
            if (cursorX >= getWidth()) break
            screen.set(cursorX, cursorY, Cell(ch, currentStyle))
            moveCursorRight(1)
        }
    }

    fun insertText(text: String) {
        for (ch in text) {
            insertCharAtCursor(ch)
            advanceCursorWithWrap()
        }
    }

    fun fillLine(row: Int, ch: Char? = null) {
        screen.fillLine(row, Cell(ch, currentStyle))
    }

    fun insertEmptyLineAtBottom() {
        val removed = screen.scrollUp()
        scrollback.add(removed)
    }

    fun clearScreen() {
        screen.clear()
        setCursorPosition(0, 0)
    }

    fun clearScreenAndScrollback() {
        screen.clear()
        scrollback.clear()
        setCursorPosition(0, 0)
    }

    fun getCharacterAt(row: Int, column: Int): Char? = getCell(row, column).char

    fun getCellAt(row: Int, column: Int): Cell = getCell(row, column)

    fun getAttributesAt(row: Int, column: Int): TextStyle = getCell(row, column).style

    fun getLineAsString(row: Int): String {
        return getLine(row)
            .joinToString(separator = "") { it.displayChar().toString() }
            .trimEnd()
    }

    fun getScreenContentAsString(): String {
        return (0 until getHeight())
            .joinToString(separator = "\n") { screenRow ->
                getLineAsString(scrollback.size() + screenRow)
            }
    }

    fun getAllContentAsString(): String {
        val totalLines = scrollback.size() + getHeight()
        return (0 until totalLines)
            .joinToString(separator = "\n") { row -> getLineAsString(row) }
    }

    fun dump(): String {
        val scrollbackDump = scrollback.asLines().joinToString("\n") { lineToString(it) }
        val screenDump = screen.asLines().joinToString("\n") { lineToString(it) }

        return buildString {
            appendLine("=== Scrollback ===")
            appendLine(scrollbackDump)
            appendLine("=== Screen ===")
            append(screenDump)
        }.trimEnd()
    }

    override fun toString(): String = dump()

    private fun insertCharAtCursor(ch: Char) {
        val row = screen.getLine(cursorY).toMutableList()
        row.add(cursorX, Cell(ch, currentStyle))

        val overflow = if (row.size > getWidth()) row.removeAt(getWidth()) else null
        screen.setLine(cursorY, row)

        val carry = overflow?.takeIf { it.char != null } ?: return

        if (cursorY < getHeight() - 1) {
            carryOverflow(cursorY + 1, carry)
        } else {
            pushCarryPastBottom(carry)
        }
    }

    private fun carryOverflow(rowIndex: Int, initialCell: Cell) {
        var currentRowIndex = rowIndex
        var carry: Cell? = initialCell

        while (carry != null && currentRowIndex < getHeight()) {
            val row = screen.getLine(currentRowIndex).toMutableList()
            row.add(0, carry)

            val overflow = if (row.size > getWidth()) row.removeAt(getWidth()) else null
            screen.setLine(currentRowIndex, row)

            carry = overflow?.takeIf { it.char != null }
            currentRowIndex++
        }

        if (carry != null) {
            pushCarryPastBottom(carry)
        }
    }

    private fun pushCarryPastBottom(cell: Cell) {
        val removed = screen.scrollUp()
        scrollback.add(removed)

        val lastRow = screen.getLine(getHeight() - 1).toMutableList()
        lastRow.add(0, cell)
        if (lastRow.size > getWidth()) {
            lastRow.removeAt(getWidth())
        }
        screen.setLine(getHeight() - 1, lastRow)
    }

    private fun advanceCursorWithWrap() {
        if (cursorX < getWidth() - 1) {
            cursorX++
        } else {
            cursorX = 0
            if (cursorY < getHeight() - 1) {
                cursorY++
            } else {
                val removed = screen.scrollUp()
                scrollback.add(removed)
            }
        }
    }

    private fun getLine(globalRow: Int): List<Cell> {
        return if (globalRow < scrollback.size()) {
            scrollback.getLine(globalRow)
        } else {
            val screenRow = globalRow - scrollback.size()
            require(screenRow in 0 until getHeight()) { "Row out of bounds: $globalRow" }
            screen.getLine(screenRow)
        }
    }

    private fun getCell(globalRow: Int, column: Int): Cell {
        require(column in 0 until getWidth()) { "Column out of bounds: $column" }
        return if (globalRow < scrollback.size()) {
            scrollback.getLine(globalRow)[column]
        } else {
            val screenRow = globalRow - scrollback.size()
            require(screenRow in 0 until getHeight()) { "Row out of bounds: $globalRow" }
            screen.get(column, screenRow)
        }
    }

    private fun lineToString(line: List<Cell>): String {
        return line.joinToString(separator = "") { it.displayChar().toString() }.trimEnd()
    }

    fun resize(newWidth: Int, newHeight: Int) {
        require(newWidth > 0) { "newWidth must be greater than 0" }
        require(newHeight > 0) { "newHeight must be greater than 0" }

        val allLines = getAllLines()
        val reflowedLines = reflowLines(allLines, newWidth)

        rebuildStorageFromLines(reflowedLines, newWidth, newHeight)

        cursorX = cursorX.coerceIn(0, newWidth - 1)
        cursorY = cursorY.coerceIn(0, newHeight - 1)
    }

    private fun getAllLines(): List<List<Cell>> {
        val allLines: List<List<Cell>> = scrollback.asLines() + screen.asLines()
        return allLines
    }

    private fun trimTrailingEmptyCells(line: List<Cell>): List<Cell> {
        var lastNonEmpty = line.size - 1
        while (lastNonEmpty >= 0 && line[lastNonEmpty].char == null) {
            lastNonEmpty--
        }
        return if (lastNonEmpty < 0) emptyList() else line.subList(0, lastNonEmpty + 1)
    }

    private fun reflowLines(lines: List<List<Cell>>, newWidth: Int): List<List<Cell>> {
        val result = mutableListOf<List<Cell>>()

        for (line in lines) {
            val trimmed = trimTrailingEmptyCells(line)

            if (trimmed.isEmpty()) {
                result.add(emptyList())
                continue
            }

            var start = 0
            while (start < trimmed.size) {
                val end = minOf(start + newWidth, trimmed.size)
                result.add(trimmed.subList(start, end))
                start = end
            }
        }

        return result
    }

    private fun rebuildStorageFromLines(
        lines: List<List<Cell>>,
        newWidth: Int,
        newHeight: Int
    ) {
        val normalizedLines = lines.map { Screen.normalizeLine(it, newWidth) }

        val totalLines = normalizedLines.ifEmpty {
            List(newHeight) { Screen.blankLine(newWidth).toList() }
        }

        val visibleStart = maxOf(0, totalLines.size - newHeight)
        val scrollbackLines = totalLines.subList(0, visibleStart)
        val screenLines = totalLines.subList(visibleStart, totalLines.size)

        screen = Screen(newWidth, newHeight)
        scrollback = Scrollback(scrollbackMaxSize)

        val keptScrollback =
            if (scrollbackLines.size > scrollbackMaxSize) {
                scrollbackLines.takeLast(scrollbackMaxSize)
            } else {
                scrollbackLines
            }

        for (line in keptScrollback) {
            scrollback.add(line)
        }

        val padTop = newHeight - screenLines.size

        for (row in 0 until newHeight) {
            val line =
                if (row < padTop) {
                    Screen.blankLine(newWidth).toList()
                } else {
                    screenLines[row - padTop]
                }
            screen.setLine(row, line)
        }
    }

}

