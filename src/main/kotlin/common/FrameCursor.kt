package com.fxynos.multiprocessing.lab1.common

/**
 * Colored frame cursor
 */
interface FrameCursor {
    val row: Int
    val column: Int
    val rows: Int
    val columns: Int

    val red: Int // 0..255
    val green: Int // 0..255
    val blue: Int // 0..255

    fun moveTo(row: Int, column: Int)
    fun setRed(value: Int) // takes
    fun setGreen(value: Int)
    fun setBlue(value: Int)
}

/**
 * Cursor for editing pixels inside BGR ordered buffered frame
 */
class BgrBufferedFrameCursor(
    private val buffer: ByteArray,
    private val channels: Int,
    override val rows: Int,
    override val columns: Int
) : FrameCursor {
    override var row = 0
        private set
    override var column = 0
        private set

    private val baseIndex: Int get() = (row * columns + column) * channels
    private val redChannelIndex: Int get() = baseIndex + 2
    private val greenChannelIndex: Int get() = baseIndex + 1
    private val blueChannelIndex: Int get() = baseIndex

    override val red: Int get() = buffer[redChannelIndex].toInt() and 0xFF
    override val green: Int get() = buffer[greenChannelIndex].toInt() and 0xFF
    override val blue: Int get() = buffer[blueChannelIndex].toInt() and 0xFF

    override fun moveTo(row: Int, column: Int) {
        this.row = row
        this.column = column
    }

    override fun setRed(value: Int) {
        buffer[redChannelIndex] = value.toByte()
    }

    override fun setGreen(value: Int) {
        buffer[greenChannelIndex] = value.toByte()
    }

    override fun setBlue(value: Int) {
        buffer[blueChannelIndex] = value.toByte()
    }
}