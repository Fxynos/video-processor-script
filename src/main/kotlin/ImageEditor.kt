package com.fxynos.multiprocessing.lab1

import com.fxynos.multiprocessing.lab1.common.ConvolutionWithRectangleColorFilterFrameEditor
import com.fxynos.multiprocessing.lab1.common.FrameEditor
import com.fxynos.multiprocessing.lab1.common.RgbFrameEditor
import com.fxynos.multiprocessing.lab1.common.SingleSizeFrameBufferSupplier
import com.fxynos.multiprocessing.lab1.common.ThreadLocalFrameBufferSupplier
import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs

fun main() {
    OpenCV.loadLocally()

    val startMs = System.currentTimeMillis()
    fun log(msg: String) = println("[${System.currentTimeMillis() - startMs} ms] $msg")

    val mat: Mat = Imgcodecs.imread("C:\\Users\\Fxynos\\Pictures\\lab\\in1080.png")
    log("Image loaded: ${mat.cols()}x${mat.rows()}, ${mat.channels()} channels")

    val editor: FrameEditor = ConvolutionWithRectangleColorFilterFrameEditor(
        bufferSupplier = ThreadLocalFrameBufferSupplier(),
        minArea = 50,
        redIgnoredRange = 200..255,
        greenIgnoredRange = 200..255,
        blueIgnoredRange = 200..255,
        convolutionMatrix = Array(15) {
            IntArray(15) { 1 }
        }
    )
    editor.edit(mat)
    log("Image updated")

    Imgcodecs.imwrite("C:\\Users\\Fxynos\\Pictures\\lab\\out1080.png", mat)
    log("Image saved")

    mat.release()
}