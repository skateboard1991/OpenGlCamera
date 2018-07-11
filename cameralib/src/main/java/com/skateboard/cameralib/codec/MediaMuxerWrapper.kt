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

    private val lock = ReentrantLock()

    private val condition=lock.newCondition()

    private var num = 0

    val TAG="MediaMuxerWrapper"

    private var isStarting = false

    fun isStarting(): Boolean
    {
        return isStarting
    }

    fun addTrack(mediaFormat: MediaFormat): Int
    {
        return mediaMuxer.addTrack(mediaFormat)
    }

    fun start()
    {
        lockAction {

            while (!isStarting)
            {
                num++
                if (num == trackNum)
                {
                    mediaMuxer.start()
                    isStarting = true
                    condition.signalAll()
                }
                else
                {
                    condition.await()
                }
            }

        }


    }

    fun writeSampleData(trackIndex: Int, byteBuf: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)
    {
        mediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo)
    }

    fun release()
    {
        lockAction {

            if(!isStarting)
            {
                mediaMuxer.release()
            }
        }

    }

    fun stop()
    {
        lockAction {
            if(isStarting)
            {
                num--
                if(num==0)
                {
                    mediaMuxer.stop()
                    isStarting=false
                }
            }
        }

    }

    fun lockAction(T: () -> Unit)
    {

        try
        {
            lock.lock()
            T()
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
        finally
        {
            lock.unlock()
        }

    }

}