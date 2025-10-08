package com.fxynos.multiprocessing.lab1

import org.opencv.core.Mat

/**
 * Buffer of [mat] size
 */
fun createBuffer(mat: Mat) = ByteArray(mat.rows() * mat.cols() * mat.channels())

/**
 * [mat] with 3 channels
 */
fun editFrame(mat: Mat, buffer: ByteArray) {
    mat.get(0, 0, buffer)

    for (i in 0 until mat.rows())
        for (j in 0 until mat.cols()) {
            val index = (i * mat.cols() + j) * mat.channels()
            val red: Int = buffer[index + 2].toInt() and 0xFF // OpenCV uses BGR ordering
            val green: Int = buffer[index + 1].toInt() and 0xFF
            val blue: Int = buffer[index].toInt() and 0xFF

            buffer[index + 2] = (red * 2.0).coerceAtMost(255.0).toInt().toByte()
            buffer[index + 1] = green.toByte()
            buffer[index] = (blue * 0.5).coerceAtLeast(0.0).toInt().toByte()
        }

    mat.put(0, 0, buffer)
}