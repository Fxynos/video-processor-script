package com.fxynos.multiprocessing.lab1

import com.fxynos.multiprocessing.lab1.common.FrameEditor
import com.fxynos.multiprocessing.lab1.common.RgbFrameEditor
import com.fxynos.multiprocessing.lab1.common.SingleSizeFrameBufferSupplier
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

private const val THREADS_COUNT_READ = 4
private const val THREADS_COUNT_EDIT = 32
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

    val executorService = Executors.newFixedThreadPool(maxOf(THREADS_COUNT_READ, THREADS_COUNT_EDIT))
    val readChunkSize: Int = frameCount / THREADS_COUNT_READ
    val allFrames: List<Mat> = executorService.invokeAll(
        List(THREADS_COUNT_READ) { threadIndex ->
            Callable {
                log("Chunk $threadIndex is reading...")
                val result = readChunk(
                    videoFilePath = VID_SRC_FILE_PATH,
                    fromFrameInclusive = threadIndex * readChunkSize,
                    toFrameExclusive = if (threadIndex == THREADS_COUNT_READ - 1)
                            frameCount
                        else
                            (threadIndex + 1) * readChunkSize
                )
                log("Chunk $threadIndex is read")
                result
            }
        }
    ).flatMap(Future<List<Mat>>::get)
    log("Frames are read")

    val editChunkSize: Int = allFrames.size / THREADS_COUNT_EDIT
    executorService.invokeAll(
        List(THREADS_COUNT_EDIT) { threadIndex ->
            Callable {
                log("Chunk $threadIndex is editing...")
                editChunk(allFrames.subList(
                    fromIndex = threadIndex * editChunkSize,
                    toIndex = if (threadIndex == THREADS_COUNT_EDIT - 1)
                        frameCount
                    else
                        (threadIndex + 1) * editChunkSize
                )) // change input frames
                log("Chunk $threadIndex is edited")
            }
        }
    )
    log("Frames are edited")

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

/**
 * Updates content of input [frames]
 */
private fun editChunk(frames: List<Mat>) {
    val editor: FrameEditor = RgbFrameEditor(
        SingleSizeFrameBufferSupplier(),
        1.5f,
        1f,
        0.5f
    )
    frames.map { editor.edit(it) }
}