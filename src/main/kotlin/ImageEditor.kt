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

    editFrame(mat, createBuffer(mat))
    log("Image updated")

    Imgcodecs.imwrite("./output.jpg", mat)
    log("Image saved")

    mat.release()
}