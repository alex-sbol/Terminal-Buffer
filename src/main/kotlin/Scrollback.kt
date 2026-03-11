package org.example

class Scrollback(var size: Int) {

    var lines = ArrayDeque<List<Cell>>()
}