package org.example

class Screen(var width: Int, var height: Int) {

    private var cells = Array(width * height) { Cell() }

    fun get(x: Int, y: Int): Cell {
        return cells[y * width + x]
    }

    fun set(x: Int, y: Int, cell: Cell) {
        cells[y * width + x] = cell
    }

}