package org.example

class Screen(var width: Int, var height: Int) {
    init {
        require(width > 0) { "Screen width must be positive" }
        require(height > 0) { "Screen height must be positive" }
    }

    private val lines: MutableList<MutableList<Cell>> =
        MutableList(height) { blankLine(width) }

    fun get(column: Int, row: Int): Cell {
        requireInBounds(column, row)
        return lines[row][column]
    }

    fun set(column: Int, row: Int, cell: Cell) {
        requireInBounds(column, row)
        lines[row][column] = cell
    }

    fun getLine(row: Int): List<Cell> {
        require(row in 0 until height) { "Row out of bounds: $row" }
        return lines[row].toList()
    }

    fun setLine(row: Int, line: List<Cell>) {
        require(row in 0 until height) { "Row out of bounds: $row" }
        lines[row] = normalizeLine(line, width).toMutableList()
    }

    fun fillLine(row: Int, cell: Cell) {
        require(row in 0 until height) { "Row out of bounds: $row" }
        lines[row] = MutableList(width) { cell }
    }

    fun clear() {
        for (row in 0 until height) {
            lines[row] = blankLine(width)
        }
    }

    fun scrollUp(): List<Cell> {
        val removed = lines.removeAt(0)
        lines.add(blankLine(width))
        return removed.toList()
    }

    fun asLines(): List<List<Cell>> = lines.map { it.toList() }

    companion object {
        fun blankLine(width: Int): MutableList<Cell> = MutableList(width) { Cell() }

        fun normalizeLine(line: List<Cell>, width: Int): List<Cell> {
            return if (line.size >= width) {
                line.take(width)
            } else {
                line + List(width - line.size) { Cell() }
            }
        }
    }

    private fun requireInBounds(column: Int, row: Int) {
        require(column in 0 until width) { "Column out of bounds: $column" }
        require(row in 0 until height) { "Row out of bounds: $row" }
    }
}