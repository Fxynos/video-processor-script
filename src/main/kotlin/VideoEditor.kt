package com.fxynos.multiprocessing.lab1

import com.fxynos.multiprocessing.lab1.common.ConvolutionWithRectangleColorFilterFrameEditor
import com.fxynos.multiprocessing.lab1.common.FrameEditor
import com.fxynos.multiprocessing.lab1.common.ThreadLocalFrameBufferSupplier
import nu.pattern.OpenCV
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.videoio.VideoCapture
import org.opencv.videoio.VideoWriter
import org.opencv.videoio.Videoio
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.decrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
fun main() {
    OpenCV.loadLocally()
    val videos = arrayOf(
        // TEST
        //"input/CBF2_TEST.mp4",

        // 360p
        //"input/CBF30.mp4",
        //"input/CBF60.mp4",
        //"input/CBF120.mp4",

        // 720p
        "input/ROE30.mp4",
        "input/ROE60.mp4",
        "input/ROE120.mp4",

        // 1080p
        "input/TLD30.mp4",
        "input/TLD60.mp4",
        "input/TLD120.mp4"
    ).map(::File)
    val cores = intArrayOf(
        32,
        24,
        16,
        12,
        8
    )
    for (video in videos)
        for (coreCount in cores) {
            println("${video.name} $coreCount cores")
            val outputName = "${video.name}_$coreCount"
            val logWriter = PrintStream(BufferedOutputStream(FileOutputStream(File(
                "logs",
                "$outputName.txt"
            ))))
            val startMs = System.currentTimeMillis()
            editVideo(
                inputPath = video.absolutePath,
                outputPath = File("output", video.name).absolutePath,
                threadsRead = 4,
                threadsEdit = coreCount
            ) {
                logWriter.println("[${Thread.currentThread().name} ${System.currentTimeMillis() - startMs} ms] $it")
            }
            logWriter.flush()
            logWriter.close()
        }
}

@OptIn(ExperimentalAtomicApi::class)
private fun editVideo(
    inputPath: String,
    outputPath: String,
    threadsRead: Int,
    threadsEdit: Int,
    log: (String) -> Unit
) {
    val capture = VideoCapture(inputPath)
    val width: Int = capture.get(Videoio.CAP_PROP_FRAME_WIDTH).toInt()
    val height: Int = capture.get(Videoio.CAP_PROP_FRAME_HEIGHT).toInt()
    val fps = capture.get(Videoio.CAP_PROP_FPS)
    val frameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT).toInt()
    log("Video loaded: ${width}x$height, ${String.format("%.2f", frameCount / fps)} seconds ($frameCount frames)")

    val executorService = Executors.newFixedThreadPool(maxOf(threadsRead, threadsEdit))
    val readChunkSize: Int = frameCount / threadsRead
    val allFrames: List<Mat> = executorService.invokeAll(
        List(threadsRead) { threadIndex ->
            Callable {
                log("Chunk $threadIndex is reading...")
                val result = readChunk(
                    videoFilePath = inputPath,
                    fromFrameInclusive = threadIndex * readChunkSize,
                    toFrameExclusive = if (threadIndex == threadsRead - 1)
                        frameCount
                    else
                        (threadIndex + 1) * readChunkSize
                )
                log("Chunk $threadIndex is read: ${result.size} frames")
                result
            }
        }
    ).flatMap(Future<List<Mat>>::get)
    log("Frames are read")

    val editor: FrameEditor = ConvolutionWithRectangleColorFilterFrameEditor(
        bufferSupplier = ThreadLocalFrameBufferSupplier(),
        minArea = 100,
        redIgnoredRange = 190..220,
        greenIgnoredRange = 40..80,
        blueIgnoredRange = 50..100,
        convolutionMatrix = arrayOf(
            intArrayOf(1,  4,  6,  4, 1),
            intArrayOf(4, 16, 24, 16, 4),
            intArrayOf(6, 24, 36, 24, 6),
            intArrayOf(4, 16, 24, 16, 4),
            intArrayOf(1,  4,  6,  4, 1)
        )
    )
    val actualFrameCount = allFrames.size // can differ from `frameCount`
    val chunksCount = (threadsEdit * 10).coerceAtMost(actualFrameCount)
    val framesPerChunk: Int = actualFrameCount / chunksCount
    val processingFramesCount = AtomicInt(chunksCount)
    log("Split to $chunksCount chunks: $framesPerChunk frames per chunk")
    executorService.invokeAll(
        List(chunksCount) { threadIndex ->
            Callable {
                log("Chunk $threadIndex is editing...")
                allFrames.subList(
                    fromIndex = threadIndex * framesPerChunk,
                    toIndex = if (threadIndex == chunksCount - 1)
                        actualFrameCount
                    else
                        (threadIndex + 1) * framesPerChunk
                ).forEach(editor::edit)
                log("Chunk $threadIndex is edited: ${processingFramesCount.decrementAndFetch()} chunks left")
            }
        }
    ).forEach(Future<Unit>::get)
    log("Frames are edited")

    VideoWriter(
        outputPath,
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
    executorService.shutdownNow()
    log("Resources released")
}

private fun readChunk(
    videoFilePath: String,
    fromFrameInclusive: Int,
    toFrameExclusive: Int
): List<Mat> = VideoCapture(videoFilePath).run {
    set(Videoio.CAP_PROP_POS_FRAMES, fromFrameInclusive.toDouble())
    val chunk = buildList {
        for (i in fromFrameInclusive until toFrameExclusive)
            Mat()
                .takeIf(::read)
                ?.apply(::add)
                ?: break
    }
    release()
    chunk
}