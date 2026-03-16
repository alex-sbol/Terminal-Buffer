package org.example

data class Cell(
    val char: Char? = null,
    val style: TextStyle = TextStyle()
) {
    fun displayChar(): Char = char ?: ' '
}