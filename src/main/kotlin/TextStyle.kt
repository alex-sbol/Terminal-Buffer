package org.example

data class TextStyle(
    val foreground: Colors = Colors.DEFAULT_FOREGROUND,
    val background: Colors = Colors.DEFAULT_BACKGROUND,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false
)