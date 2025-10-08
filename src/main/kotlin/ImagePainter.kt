package com.fxynos.multiprocessing.lab1

import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs

fun main() {
    OpenCV.loadLocally()

    val startMs = System.currentTimeMillis()
    fun log(msg: String) = println("[${System.currentTimeMillis() - startMs} ms] $msg")

    val mat: Mat = Imgcodecs.imread("C:\\Program Files (x86)\\Steam\\userdata\\1103990303\\760\\remote\\305620\\screenshots\\20250914161938_1.jpg")
    log("Image loaded: ${mat.cols()}x${mat.rows()}, ${mat.channels()} channels")

    val buffer = ByteArray(mat.rows() * mat.cols() * mat.channels())
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
    log("Buffered image edited")

    mat.put(0, 0, buffer)
    log("Image updated")

    Imgcodecs.imwrite("./output.jpg", mat)
    log("Image saved")
}