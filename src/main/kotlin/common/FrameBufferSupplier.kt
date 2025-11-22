package com.fxynos.multiprocessing.lab1.common

import org.opencv.core.Mat
import kotlin.concurrent.getOrSet

abstract class FrameBufferSupplier {
    abstract fun supply(frame: Mat): ByteArray
    protected fun createBuffer(frame: Mat) = ByteArray(frame.rows() * frame.cols() * frame.channels())
}

/**
 * Uses first frame to calculate buffer size and assumes that all following frames have the same size.
 */
class SingleSizeFrameBufferSupplier : FrameBufferSupplier() {

    private var buffer: ByteArray? = null

    override fun supply(frame: Mat): ByteArray =
        buffer ?:
        createBuffer(frame).also { buffer = it }
}

/**
 * Like [SingleSizeFrameBufferSupplier] but concurrent
 */
class ThreadLocalFrameBufferSupplier : FrameBufferSupplier() {

    private val buffer = ThreadLocal<ByteArray>()

    override fun supply(frame: Mat): ByteArray =
        buffer.getOrSet { createBuffer(frame) }
}