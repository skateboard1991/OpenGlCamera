package com.skateboard.cameralib.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

class MediaMuxerWrapper(private val mediaMuxer: MediaMuxer, private val trackNum: Int)
{

    private var countDownLatch = CountDownLatch(trackNum)

    private val stopCyclicBarrier = CyclicBarrier(trackNum)
    {
        mediaMuxer.stop()
    }

    private val releaseCyclicBarrier = CyclicBarrier(trackNum) {

        mediaMuxer.release()
    }

    private var isStarting = false

    fun isStarting():Boolean
    {
        return isStarting
    }

    fun addTrack(mediaFormat: MediaFormat): Int
    {

        val index = mediaMuxer.addTrack(mediaFormat)
        countDownLatch.countDown()
        return index
    }

    fun start()
    {
        countDownLatch.await()
        if (!isStarting)
        {
            isStarting = true
            mediaMuxer.start()
        }
    }

    fun writeSampleData(trackIndex: Int, byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)
    {
        mediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo)
    }

    fun release()
    {
        isStarting = false
        releaseCyclicBarrier.await()
    }

    fun stop()
    {
        isStarting = false
        stopCyclicBarrier.await()

    }


}