package com.skateboard.cameralib.codec

import android.media.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and

class CameraRecorder
{
    private val LOCK = Any()
    private lateinit var videoCodec: MediaCodec
    private lateinit var mediaMuxer: MediaMuxer
    val VIDEO_MINE = "video/avc"
    val BIT_RATE = 2048000       //视频编码波特率
    val FRAME_RATE = 24           //视频编码帧率
    private val IFRAME_INTERVAL = 1
    private var videoThread: Thread? = null

    private var cancelFlag = false

    private var startingFlag = false

    private val fpsTime: Long

    private var filePath: String = ""

    val TAG = "CAMERARECORDER"

    private var width = 0

    private var height = 0

    private var nowFeedData: ByteArray = byteArrayOf()

    init
    {
        fpsTime = 1000L / FRAME_RATE
    }

    private var nanoTime: Long = 0
    private var videoTrack = -1
    private var hasNewData = false
    private var yuv = byteArrayOf()

    fun prepare(width: Int, height: Int, filePath: String)
    {
        this.width = width
        this.height = height
        this.filePath = filePath
        mediaMuxer = MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val videoFormat = MediaFormat.createVideoFormat(VIDEO_MINE, width, height)
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, checkColorFormat(VIDEO_MINE))
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)

        videoCodec = MediaCodec.createEncoderByType(VIDEO_MINE)
        videoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        val bundle = Bundle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, BIT_RATE)
            videoCodec.setParameters(bundle)
        }
    }

    fun start()
    {
        nanoTime = System.nanoTime()
        synchronized(LOCK)
        {
            if (videoThread != null && videoThread?.isAlive == true)
            {
                startingFlag = false
                videoThread?.join()
            }
            videoCodec.start()
            startingFlag = true
            videoThread = Thread(Runnable {
                while (!cancelFlag)
                {
                    val time = System.currentTimeMillis()
                    if (nowFeedData != null)
                    {
                        try
                        {
                            if (videoStep(nowFeedData))
                            {
                                break
                            }
                        } catch (e: IOException)
                        {
                            e.printStackTrace()
                        }

                    }
                    val lt = System.currentTimeMillis() - time
                    if (fpsTime > lt)
                    {
                        try
                        {
                            Thread.sleep(fpsTime - lt)
                        } catch (e: InterruptedException)
                        {
                            e.printStackTrace()
                        }

                    }
                }
            })
            videoThread?.start()
        }
    }

    private fun videoStep(nowFeedData: ByteArray): Boolean
    {
        val index = videoCodec.dequeueInputBuffer(-1)
        if (index >= 0)
        {
            if (hasNewData)
            {
                yuv = ByteArray(width * height * 3 / 2)
                rgbaToYuv(nowFeedData, width, height, yuv)
            }
            val buffer = getInputBuffer(videoCodec, index)
            buffer.clear()
            buffer.put(nowFeedData)
            videoCodec.queueInputBuffer(index, 0, nowFeedData.size, (System.nanoTime() - nanoTime) / 1000, if (startingFlag) 0 else MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        val mInfo = MediaCodec.BufferInfo()
        var outIndex = videoCodec.dequeueOutputBuffer(mInfo, 0)
        do
        {
            if (outIndex >= 0)
            {
                val outBuf = getOutputBuffer(videoCodec, outIndex)
                if (videoTrack >= 0 && mInfo.size > 0 && mInfo.presentationTimeUs > 0)
                {
                    try
                    {
                        mediaMuxer.writeSampleData(videoTrack, outBuf, mInfo)
                    } catch (e: Exception)
                    {
                        Log.e(TAG, "video error:size=" + mInfo.size + "/offset="
                                + mInfo.offset + "/timeUs=" + mInfo.presentationTimeUs)
                        //e.printStackTrace();
                        Log.e(TAG, "-->" + e.message)
                    }

                }
                videoCodec.releaseOutputBuffer(outIndex, false)
                outIndex = videoCodec.dequeueOutputBuffer(mInfo, 0)
                if (mInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                {
                    Log.e(TAG, "video end")
                    return true
                }
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            {
                videoTrack = mediaMuxer.addTrack(videoCodec.outputFormat)
                Log.e(TAG, "add video track-->$videoTrack")
                if (videoTrack >= 0)
                {
                    mediaMuxer.start()
                }
            }
        } while (outIndex >= 0)
        return false
    }

    private fun getInputBuffer(codec: MediaCodec, index: Int): ByteBuffer
    {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            codec.getInputBuffer(index)
        } else
        {
            codec.inputBuffers[index]
        }
    }

    private fun getOutputBuffer(codec: MediaCodec, index: Int): ByteBuffer?
    {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            codec.getOutputBuffer(index)
        } else
        {
            codec.outputBuffers[index]
        }
    }

    private fun rgbaToYuv(rgba: ByteArray, width: Int, height: Int, yuv: ByteArray)
    {
        val frameSize = width * height

        var yIndex = 0
        var uIndex = frameSize
        var vIndex = frameSize + frameSize / 4

        var R: Int
        var G: Int
        var B: Int
        var Y: Int
        var U: Int
        var V: Int
        var index = 0
        for (j in 0 until height)
        {
            for (i in 0 until width)
            {
                index = j * width + i
                if (rgba[index * 4] > 127 || rgba[index * 4] < -128)
                {
                    Log.e("color", "-->" + rgba[index * 4])
                }
                R = rgba[index * 4].toInt() and 0xFF
                G = rgba[index * 4 + 1].toInt() and (0xFF)
                B = rgba[index * 4 + 2].toInt() and 0xFF

                Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128

                yuv[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
                if (j % 2 == 0 && index % 2 == 0)
                {
                    yuv[uIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                    yuv[vIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                }
            }
        }
    }

    fun feedData(data: ByteArray)
    {
        hasNewData = true
        nowFeedData = data
        //        nowTimeStep=timeStep;
    }

    fun cancel()
    {
        cancelFlag = true
        stop()
        cancelFlag = false
        val file = File(filePath)
        if (file.exists())
        {
            file.delete()
        }
    }

    fun stop()
    {
        try
        {
            synchronized(LOCK) {

                startingFlag = false
                videoThread?.join()

                videoCodec.stop()
                videoCodec.release()

                //Muxer Stop
                videoTrack = -1
                mediaMuxer.stop()
                mediaMuxer.release()
            }
        } catch (e: Exception)
        {
            e.printStackTrace()
        }

    }


    private fun checkColorFormat(mime: String): Int
    {
//        if (Build.MODEL == "HUAWEI P6-C00")
//        {
//            convertType = DataConvert.BGRA_YUV420SP
//            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
//        }
        for (i in 0 until MediaCodecList.getCodecCount())
        {
            val info = MediaCodecList.getCodecInfoAt(i)
            if (info.isEncoder)
            {
                val types = info.supportedTypes
                for (type in types)
                {
                    if (type == mime)
                    {
                        Log.e("YUV", "type-->$type")
                        val c = info.getCapabilitiesForType(type)
                        Log.e("YUV", "color-->" + Arrays.toString(c.colorFormats))
                        for (j in c.colorFormats.indices)
                        {
                            if (c.colorFormats[j] == MediaCodecInfo.CodecCapabilities
                                            .COLOR_FormatYUV420Planar)
                            {
                                return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                            } else if (c.colorFormats[j] == MediaCodecInfo.CodecCapabilities
                                            .COLOR_FormatYUV420SemiPlanar)
                            {
                                return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                            }
                        }
                    }
                }
            }
        }
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }

}