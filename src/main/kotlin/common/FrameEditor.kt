package com.fxynos.multiprocessing.lab1.common

import org.opencv.core.Mat
import java.util.LinkedList
import kotlin.collections.ArrayDeque

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
 * and satisfy color range: [redIgnoredRange], [greenIgnoredRange], [blueIgnoredRange].
 * The [convolutionMatrix] is matrix NxN where N is odd.
 */
class ConvolutionWithRectangleColorFilterFrameEditor(
    bufferSupplier: FrameBufferSupplier,

    val minArea: Int,
    val redIgnoredRange: IntRange,
    val greenIgnoredRange: IntRange,
    val blueIgnoredRange: IntRange,
    val convolutionMatrix: Array<IntArray>,
    val convolutionDiv: Int = convolutionMatrix.sumOf(IntArray::sum)
) : FrameEditor(bufferSupplier) {
    private val matrixRadius = convolutionMatrix.size / 2
    private val FrameCursor.isPixelSatisfiesIgnoreCondition: Boolean
        get() = red in redIgnoredRange && green in greenIgnoredRange && blue in blueIgnoredRange

    override fun editBuffered(cursor: FrameCursor): Unit =
        editBufferedIgnoringAreas(cursor, getIgnoredAreas(cursor))

    private fun editBufferedIgnoringAreas(cursor: FrameCursor, areas: List<IgnoredArea>) {
        for (row in 0 until cursor.rows)
            for (column in 0 until cursor.columns)
                if (areas.none { it.contains(row, column) }) {
                    cursor.moveTo(row, column)
                    cursor.applyConvolutionToCurrentPosition()
                }
    }

    private fun FrameCursor.applyConvolutionToCurrentPosition() {
        val targetRow = row
        val targetColumn = column

        var convolutionSumRed = 0
        var convolutionSumGreen = 0
        var convolutionSumBlue = 0

        for (rowDelta in (-matrixRadius) .. matrixRadius)
            for (columnDelta in (-matrixRadius) .. matrixRadius) {
                moveTo( // mirror neighboring pixels if index out of bounds
                    row = (targetRow + rowDelta).coerceIn(0, rows - 1),
                    column = (targetColumn + columnDelta).coerceIn(0, columns - 1)
                )
                convolutionSumRed += red * convolutionMatrix[rowDelta + matrixRadius][columnDelta + matrixRadius]
                convolutionSumGreen += green * convolutionMatrix[rowDelta + matrixRadius][columnDelta + matrixRadius]
                convolutionSumBlue += blue * convolutionMatrix[rowDelta + matrixRadius][columnDelta + matrixRadius]
            }

        moveTo(targetRow, targetColumn)
        setRed(convolutionSumRed / convolutionDiv)
        setGreen(convolutionSumGreen / convolutionDiv)
        setBlue(convolutionSumBlue / convolutionDiv)
    }

    private fun getIgnoredAreas(cursor: FrameCursor): List<IgnoredArea> {
        val ignoredAreas = LinkedList<IgnoredArea>()
        for (row in 0 until cursor.rows)
            for (column in 0 until cursor.columns)
                if (ignoredAreas.none { it.contains(row, column) }) {
                    cursor.moveTo(row, column)
                    if (cursor.isPixelSatisfiesIgnoreCondition)
                        ignoredAreas += getIgnoredArea(cursor, row, column)
                }
        return ignoredAreas.filter { it.area >= minArea }
    }


    private fun getIgnoredArea(cursor: FrameCursor, startRow: Int, startColumn: Int): IgnoredArea {
        fun Int.row() = this / cursor.columns
        fun Int.column() = this % cursor.columns
        fun position(row: Int, column: Int) = row * cursor.columns + column

        val areaPixels = LinkedList<Int>() // item = row * cols + column
        val queueForChecking = ArrayDeque(listOf(position(startRow, startColumn)))

        var minRow: Int? = null
        var maxRow: Int? = null
        var minCol: Int? = null
        var maxCol: Int? = null

        while (queueForChecking.isNotEmpty()) {
            val position = queueForChecking.removeFirst()
            cursor.moveTo(position.row(), position.column())

            if (!cursor.isPixelSatisfiesIgnoreCondition || areaPixels.contains(position))
                continue

            areaPixels += position

            if (minRow == null || cursor.row < minRow)
                minRow = cursor.row
            if (maxRow == null || cursor.row > maxRow)
                maxRow = cursor.row
            if (minCol == null || cursor.column < minCol)
                minCol = cursor.column
            if (maxCol == null || cursor.column > maxCol)
                maxCol = cursor.column

            // add neighboring pixels
            (-1 .. 1).asSequence()
                .map { cursor.row + it }
                .filter { it in 0 until cursor.rows }
                .flatMap { row ->
                    (-1 .. 1).asSequence()
                        .map { cursor.column + it }
                        .filter { it in 0 until cursor.columns }
                        .map { row to it }
                }.map{ (row, column) -> position(row, column) }
                .forEach(queueForChecking::add)
        }

        return IgnoredArea(
            rows = minRow!! .. maxRow!!,
            columns = minCol!! .. maxCol!!,
            area = areaPixels.size
        )
    }

    private data class IgnoredArea(val rows: IntRange, val columns: IntRange, val area: Int) {
        fun contains(row: Int, column: Int): Boolean =
            row in rows && column in columns
    }
}

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