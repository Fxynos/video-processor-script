package com.fxynos.multiprocessing.lab1

import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.VideoWriter
import org.opencv.videoio.Videoio
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val THREADS_COUNT = 4
private const val VID_SRC_FILE_PATH = "C:\\Users\\Fxynos\\Downloads\\mp lab video\\Cat brain failure 120s.mp4"
private const val VID_DEST_FILE_PATH = "output.mp4"

@OptIn(ExperimentalAtomicApi::class)
fun main() {
    OpenCV.loadLocally()

    val startMs = System.currentTimeMillis()
    fun log(msg: String) = println("[${Thread.currentThread().name} ${System.currentTimeMillis() - startMs} ms] $msg")

    val capture = VideoCapture(VID_SRC_FILE_PATH)
    val width: Int = capture.get(Videoio.CAP_PROP_FRAME_WIDTH).toInt()
    val height: Int = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT).toInt()
    val fps = capture.get(Videoio.CAP_PROP_FPS)
    val frameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT).toInt()
    log("Video loaded: ${width}x$height, ${String.format("%.2f", frameCount / fps)} seconds")

    val executorService = Executors.newFixedThreadPool(THREADS_COUNT)
    val chunkSize: Int = frameCount / THREADS_COUNT
    val allFrames: List<Mat> = executorService.invokeAll(
        List(THREADS_COUNT) { threadIndex ->
            Callable {
                log("Chunk $threadIndex reading...")
                val result = readChunk(
                    videoFilePath = VID_SRC_FILE_PATH,
                    fromFrameInclusive = threadIndex * chunkSize,
                    toFrameExclusive = if (threadIndex == THREADS_COUNT - 1)
                            frameCount
                        else
                            (threadIndex + 1) * chunkSize
                )
                log("Chunk $threadIndex is read")
                result
            }
        }
    ).flatMap(Future<List<Mat>>::get)
    log("Frames are read")

    VideoWriter(
        VID_DEST_FILE_PATH,
        capture.get(Videoio.CAP_PROP_FOURCC).toInt(), // mp4v
        fps,
        Size(
            width.toDouble(),
            height.toDouble()
        ),
        true
    ).apply {
        allFrames.forEach(::write)
        log("Video saved")
    }.release()

    allFrames.forEach(Mat::release)
    capture.release()
    executorService.shutdown()
    log("Resources released")
}

private fun readChunk(
    videoFilePath: String,
    fromFrameInclusive: Int,
    toFrameExclusive: Int
): List<Mat> = VideoCapture(videoFilePath).run {
    set(Videoio.CAP_PROP_POS_FRAMES, fromFrameInclusive.toDouble())
    List(toFrameExclusive - fromFrameInclusive) { frameIndex ->
        Mat().also {
            if (!read(it))
                throw IndexOutOfBoundsException("Frame $frameIndex is out of bounds")
        }
    }
}