package com.fxynos.multiprocessing.lab1

import com.fxynos.multiprocessing.lab1.common.FrameEditor
import com.fxynos.multiprocessing.lab1.common.RgbFilterFrameEditor
import com.fxynos.multiprocessing.lab1.common.SingleSizeFrameBufferSupplier
import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.VideoWriter
import org.opencv.videoio.Videoio

fun main() {
    OpenCV.loadLocally()

    val startMs = System.currentTimeMillis()
    fun log(msg: String) = println("[${System.currentTimeMillis() - startMs} ms] $msg")

    val capture = VideoCapture("C:\\Users\\Fxynos\\Downloads\\mp lab video\\Cat brain failure 120s.mp4")
    val width: Int = capture.get(Videoio.CAP_PROP_FRAME_WIDTH).toInt()
    val height: Int = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT).toInt()
    val fps = capture.get(Videoio.CAP_PROP_FPS)
    val frameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT)

    val output = VideoWriter(
        "output.mp4",
        capture.get(Videoio.CAP_PROP_FOURCC).toInt(), // mp4v
        fps,
        Size(
            width.toDouble(),
            height.toDouble()
        ),
        true
    )
    log("Video loaded: ${width}x$height, ${String.format("%.2f", frameCount / fps)} seconds")

    val editor: FrameEditor = RgbFilterFrameEditor(
        SingleSizeFrameBufferSupplier(),
        1.5f,
        1f,
        0.5f
    )
    var framesProcessed = 0
    val mat = Mat()
    while (capture.read(mat)) {
        editor.edit(mat)
        output.write(mat)
        log("${++framesProcessed} frames processed")
    }

    mat.release()
    capture.release()
    output.release()
    log("Video saved")
}