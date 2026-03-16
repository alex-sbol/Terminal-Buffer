package org.example

class Scrollback(val size: Int) {

    private val lines = ArrayDeque<List<Cell>>()

    fun size(): Int = lines.size

    fun asLines(): List<List<Cell>> = lines.toList()

    fun clear() { lines.clear()}


    fun add(line: List<Cell>) {
        if (size <= 0) return
        if (lines.size == size) {
            lines.removeFirst()
        }
        lines.addLast(line.toList())
    }

    fun getLine(index: Int): List<Cell> {
        require(index in 0 until lines.size) { "Scrollback line out of bounds: $index" }
        return lines.elementAt(index)
    }

}