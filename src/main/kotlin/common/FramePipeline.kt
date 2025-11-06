package com.fxynos.multiprocessing.lab1.common

import java.util.LinkedList
import java.util.concurrent.LinkedBlockingQueue

interface FramePipeline {
    fun add(frame: IndexedFrame)
    fun pop(): IndexedFrame?
    fun toList(): List<IndexedFrame>
}

class ConcurrentFramePipeline : FramePipeline {

    private val queue = LinkedBlockingQueue<IndexedFrame>()

    override fun add(frame: IndexedFrame) = queue.put(frame)
    override fun pop(): IndexedFrame? = queue.poll()
    override fun toList(): List<IndexedFrame> = LinkedList(queue)
}