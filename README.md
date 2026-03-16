# Terminal Buffer

A Kotlin implementation of the core text buffer used by a terminal emulator.

## Overview

The project models the two logical parts of a terminal buffer:

- **Screen**: the visible editable area with fixed width and height
- **Scrollback**: lines that scrolled off the top of the screen and remain available for history

Each cell stores:

- a character or empty value
- foreground color
- background color
- style flags: bold, italic, underline

The buffer also tracks:

- the current cursor position
- the current text attributes used for future edits

## Project structure

```text
src/main/kotlin/org/example/
├── Cell.kt
├── Colors.kt
├── CursorPosition.kt
├── Screen.kt
├── Scrollback.kt
├── TerminalBuffer.kt
└── TextStyle.kt

src/test/kotlin/org/example/
└── TerminalBufferTest.kt
```

## Design decisions

### 1. Overwrite vs insert

The assignment asks for both writing and inserting text. In this implementation:

- `writeText(text)` overwrites existing content on the current line and moves the cursor, every char that is over the screen width will overwrite the previous char
- `insertText(text)` inserts at the cursor, shifts content right, and moves overflow into following lines

### 2. Global row indexing

Read operations use a **global row index** across scrollback and screen:

- rows `0 .. scrollbackSize - 1` refer to scrollback
- rows `scrollbackSize .. scrollbackSize + screenHeight - 1` refer to the visible screen

This makes methods such as `getLineAsString()` and `getCharacterAt()` work for both regions.

### 3. Scrollback is immutable from the public API

Once a line scrolls into history it is preserved and can only be read, not edited.

## Supported operations

### Setup

- configurable width and height
- configurable maximum scrollback size
- constructor validation for invalid dimensions

### Attributes

- set current foreground/background/style flags
- future edits use the current style

### Cursor

- get and set cursor position
- move cursor in all four directions
- cursor is clamped to screen bounds

### Editing

- write text
- insert text
- fill a line with a character or empty cells
- insert empty line at bottom
- clear screen
- clear screen and scrollback

### Content access

- get character at position
- get cell at position
- get style at position
- get line as string
- get screen content as string
- get full scrollback + screen content as string
- debug dump for development/testing

## Build and test

### Requirements

- JDK 17+
- Gradle

### Run tests

```bash
./gradlew test
```

## Example

```kotlin
val buffer = TerminalBuffer(screenWidth = 5, screenHeight = 2, scrollbackMaxLines = 10)

buffer.setAttributes(foreground = Colors.GREEN, bold = true)
buffer.writeText("abc")
buffer.setCursorPosition(1, 0)
buffer.insertText("Z")

println(buffer.getScreenContentAsString())
```

Expected visible screen:

```text
aZbc

```

## Test coverage

The JUnit test suite covers:

- initialization and validation
- current style handling
- cursor bounds and movement
- overwrite behavior
- insert behavior and overflow handling
- fill line behavior
- scrollback behavior and capacity limits
- clear operations
- reading from screen and scrollback
- edge cases and debug output

## Improvements

- I think that Right now the class Terminal buffer handles too much functionality. I would look into refactoring some functions such that TerminalBuffer would instead call more functionality of Screen and Scrollback instead.

- The cursor can also become its own class and everything cursor related should be handled inside it. This would improve separation of concerns.

- Abstraction for line of cells could be introduced.