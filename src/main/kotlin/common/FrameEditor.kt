package com.fxynos.multiprocessing.lab1.common

import org.opencv.core.Mat

abstract class FrameEditor(val bufferSupplier: FrameBufferSupplier) {
    fun edit(frame: Mat) {
        val buffer = bufferSupplier.supply(frame)
        frame.get(0, 0, buffer)
        editBuffered(
            buffer,
            rows = frame.rows(),
            cols = frame.cols(),
            channels = frame.channels()
        )
        frame.put(0, 0, buffer)
    }

    protected abstract fun editBuffered(buffer: ByteArray, rows: Int, cols: Int, channels: Int)
}

class RgbFilterFrameEditor(
    bufferSupplier: FrameBufferSupplier,

    val redCoefficient: Float,
    val greenCoefficient: Float,
    val blueCoefficient: Float
) : FrameEditor(bufferSupplier) {
    companion object {
        private fun multiplyChannelAndCastToByte(value: Int, coefficient: Float): Byte =
            (value * coefficient)
                .coerceIn(0f, 255f)
                .toInt()
                .toByte()
    }

    override fun editBuffered(buffer: ByteArray, rows: Int, cols: Int, channels: Int) {
        for (i in 0 until rows)
            for (j in 0 until cols) {
                val index = (i * cols + j) * channels
                // get channels values in 0..255 as int
                val red: Int = buffer[index + 2].toInt() and 0xFF // OpenCV uses BGR ordering
                val green: Int = buffer[index + 1].toInt() and 0xFF
                val blue: Int = buffer[index].toInt() and 0xFF

                buffer[index + 2] = multiplyChannelAndCastToByte(red, redCoefficient)
                buffer[index + 1] = multiplyChannelAndCastToByte(green, greenCoefficient)
                buffer[index] = multiplyChannelAndCastToByte(blue, blueCoefficient)
            }
    }
}