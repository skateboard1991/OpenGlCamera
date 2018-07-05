package com.skateboard.cameralib.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.locks.ReentrantLock

class MediaMuxerWrapper(private val mediaMuxer: MediaMuxer, private val trackNum: Int)
{

    private val lock=ReentrantLock()

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
        try
        {
            lock.lock()
            if (!isStarting)
            {
                mediaMuxer.start()
                isStarting = true
            }
        }
        catch (e:Exception)
        {
            e.printStackTrace()
        }
        finally
        {
            lock.unlock()
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