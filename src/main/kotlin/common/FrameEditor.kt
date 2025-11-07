package com.fxynos.multiprocessing.lab1.common

import org.opencv.core.Mat
import java.util.LinkedList

abstract class FrameEditor(val bufferSupplier: FrameBufferSupplier) {
    fun edit(frame: Mat) {
        val buffer = bufferSupplier.supply(frame)
        frame.get(0, 0, buffer)
        editBuffered(BgrBufferedFrameCursor(
            buffer = buffer,
            rows = frame.rows(),
            columns = frame.cols(),
            channels = frame.channels()
        ))
        frame.put(0, 0, buffer)
    }

    protected abstract fun editBuffered(cursor: FrameCursor)
}

/**
 * Edit frame ignoring rectangle areas that >= [minArea]
 * and satisfy color range: [redIgnoredRange], [greenIgnoredRange], [blueIgnoredRange]
 */
//class ConvolutionWithRectangleColorFilterFrameEditor(
//    bufferSupplier: FrameBufferSupplier,
//
//    val minArea: Int,
//    val redIgnoredRange: IntRange,
//    val greenIgnoredRange: IntRange,
//    val blueIgnoredRange: IntRange
//) : FrameEditor(bufferSupplier) {
//
//    override fun editBuffered(cursor: FrameCursor) {
//        TODO("Not yet implemented")
//    }
//
//    private fun getIgnoredAreas(buffer): List<IgnoredArea> {
//        val ignoredAreas = LinkedList<IgnoredArea>()
//        for (row in )
//    }
//
//    private data class IgnoredArea(val rows: IntRange, val cols: IntRange)
//    private data class PixelPosition(val row: Int, val column: Int)
//}

/**
 * Apply color filter by multiplying RGB channels
 */
class RgbFrameEditor(
    bufferSupplier: FrameBufferSupplier,

    val redCoefficient: Float,
    val greenCoefficient: Float,
    val blueCoefficient: Float
) : FrameEditor(bufferSupplier) {
    companion object {
        private infix fun Int.channelMultipliedBy(coefficient: Float) =
            (this * coefficient)
                .coerceIn(0f, 255f)
                .toInt()
    }

    override fun editBuffered(cursor: FrameCursor) {
        for (row in 0 until cursor.rows)
            for (column in 0 until cursor.columns) {
                cursor.moveTo(row, column)
                cursor.setRed(cursor.red channelMultipliedBy redCoefficient)
                cursor.setGreen(cursor.green channelMultipliedBy greenCoefficient)
                cursor.setBlue(cursor.blue channelMultipliedBy blueCoefficient)
            }
    }
}