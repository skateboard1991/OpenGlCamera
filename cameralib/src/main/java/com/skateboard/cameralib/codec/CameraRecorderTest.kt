package com.skateboard.cameralib.codec

import android.annotation.TargetApi
import android.media.*
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.skateboard.jni.DataConvert
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class CameraRecorderTest
{
    private val LOCK = Any()

    private lateinit var mMuxer: MediaMuxer  //多路复用器，用于音视频混合
    private var path: String? = null        //文件保存的路径
    private var postfix: String? = null     //文件后缀

    private val audioMime = "audio/mp4a-latm"   //音频编码的Mime
    private lateinit var mRecorder: AudioRecord   //录音器
    private lateinit var mAudioEnc: MediaCodec   //编码器，用于音频编码
    private val audioRate = 128000   //音频编码的密钥比特率
    private val sampleRate = 48000   //音频采样率
    private val channelCount = 2     //音频编码通道数
    private val channelConfig = AudioFormat.CHANNEL_IN_STEREO   //音频录制通道,默认为立体声
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT //音频录制格式，默认为PCM16Bit

    //    private byte[] buffer;
    private var isRecording: Boolean = false
    private var bufferSize: Int = 0

    private var convertType: Int = 0

    private var mAudioThread: Thread? = null

    private lateinit var mVideoEnc: MediaCodec
    private val videoMime = "video/avc"   //视频编码格式
    private val videoRate = 2048000       //视频编码波特率
    private val frameRate = 24           //视频编码帧率
    private val frameInterval = 1        //视频编码关键帧，1秒一关键帧

    private val fpsTime: Int

    private var mVideoThread: Thread? = null
    private var mStartFlag = false
    private var width: Int = 0
    private var height: Int = 0
    //    private byte[] mHeadInfo=null;

    private var nowFeedData: ByteArray? = null
    //    private long nowTimeStep;
    private var hasNewData = false

    private var mAudioTrack = -1
    private var mVideoTrack = -1
    private val isStop = true

    private var nanoTime: Long = 0

    private var cancelFlag = false
    private val isAlign = false

    internal var yuv: ByteArray = byteArrayOf()

    init
    {
        fpsTime = 1000 / frameRate
    }

    fun setSavePath(path: String, postfix: String)
    {
        this.path = path
        this.postfix = postfix
    }

    @Throws(IOException::class)
    fun prepare(width: Int, height: Int): Int
    {
        //准备Audio
        val format = MediaFormat.createAudioFormat(audioMime, sampleRate, channelCount)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, checkColorFormat(videoMime))
        format.setInteger(MediaFormat.KEY_BIT_RATE, audioRate)
        mAudioEnc = MediaCodec.createEncoderByType(audioMime)
        mAudioEnc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2
        //        buffer=new byte[bufferSize];
        mRecorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig,
                audioFormat, bufferSize)

        //准备Video
        //        mHeadInfo=null;
        this.width = width
        this.height = height
        val videoFormat = MediaFormat.createVideoFormat(videoMime, width, height)
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoRate)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frameInterval)

        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, COLOR_FormatYUV420Planar)
        mVideoEnc = MediaCodec.createEncoderByType(videoMime)
        mVideoEnc.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val bundle = Bundle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, videoRate)
            mVideoEnc.setParameters(bundle)
        }

        mMuxer = MediaMuxer("$path.$postfix", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        return 0
    }

    @Throws(InterruptedException::class)
    fun start(): Int
    {
        //记录起始时间
        nanoTime = System.nanoTime()
        synchronized(LOCK) {
            //Audio Start
            if (mAudioThread != null && mAudioThread!!.isAlive)
            {
                isRecording = false
                mAudioThread!!.join()
            }
            if (mVideoThread != null && mVideoThread!!.isAlive)
            {
                mStartFlag = false
                mVideoThread!!.join()
            }

            mAudioEnc.start()
            mRecorder.startRecording()
            isRecording = true
            mAudioThread = Thread(Runnable {
                while (!cancelFlag)
                {
                    try
                    {
                        if (audioStep())
                        {
                            break
                        }
                    } catch (e: IOException)
                    {
                        e.printStackTrace()
                    }

                }
            })
            mAudioThread?.start()


            mVideoEnc.start()
            mStartFlag = true
            mVideoThread = Thread(Runnable {
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
            mVideoThread?.start()
        }
        return 0
    }

    fun cancel()
    {
        cancelFlag = true
        stop()
        cancelFlag = false
        val file = File(path!!)
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
                isRecording = false
                mAudioThread?.join()
                mStartFlag = false
                mVideoThread?.join()
                //Audio Stop
                mRecorder.stop()
                mAudioEnc.stop()
                mAudioEnc.release()

                //Video Stop
                mVideoEnc.stop()
                mVideoEnc.release()

                //Muxer Stop
                mVideoTrack = -1
                mAudioTrack = -1
                mMuxer.stop()
                mMuxer.release()
            }
        } catch (e: Exception)
        {
            e.printStackTrace()
        }

    }

    /**
     * 由外部喂入一帧数据
     * @param data RGBA数据
     * @param timeStep camera附带时间戳
     */
    fun feedData(data: ByteArray, timeStep: Long)
    {
        hasNewData = true
        nowFeedData = data
        //        nowTimeStep=timeStep;
    }

    private fun getInputBuffer(codec: MediaCodec, index: Int): ByteBuffer?
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

    //TODO Add End Flag
    @Throws(IOException::class)
    private fun audioStep(): Boolean
    {
        val index = mAudioEnc.dequeueInputBuffer(-1)
        if (index >= 0)
        {
            val buffer = getInputBuffer(mAudioEnc, index)
            buffer!!.clear()
            val length = mRecorder.read(buffer, bufferSize)
            if (length > 0)
            {
                mAudioEnc.queueInputBuffer(index, 0, length, (System.nanoTime() - nanoTime) / 1000, if (isRecording) 0 else MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            } else
            {
                Log.e("wuwang", "length-->$length")
            }
        }
        val mInfo = MediaCodec.BufferInfo()
        var outIndex: Int
        do
        {
            outIndex = mAudioEnc.dequeueOutputBuffer(mInfo, 0)
            Log.e("wuwang", "audio flag---->" + mInfo.flags + "/" + outIndex)
            if (outIndex >= 0)
            {
                val buffer = getOutputBuffer(mAudioEnc, outIndex)
                buffer?.position(mInfo.offset)
                //                byte[] temp=new byte[mInfo.size+7];
                //                buffer.get(temp,7,mInfo.size);
                //                addADTStoPacket(temp,temp.length);
                if (mAudioTrack >= 0 && mVideoTrack >= 0 && mInfo.size > 0 && mInfo.presentationTimeUs > 0)
                {
                    try
                    {
                        mMuxer.writeSampleData(mAudioTrack, buffer, mInfo)
                    } catch (e: Exception)
                    {
                        Log.e(TAG, "audio error:size=" + mInfo.size + "/offset="
                                + mInfo.offset + "/timeUs=" + mInfo.presentationTimeUs)
                        e.printStackTrace()
                    }

                }
                mAudioEnc.releaseOutputBuffer(outIndex, false)
                if (mInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                {
                    Log.e(TAG, "audio end")
                    return true
                }
            } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
            {

            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            {
                mAudioTrack = mMuxer.addTrack(mAudioEnc.outputFormat)
                Log.e(TAG, "add audio track-->$mAudioTrack")
                if (mAudioTrack >= 0 && mVideoTrack >= 0)
                {
                    mMuxer.start()
                }
            }
        } while (outIndex >= 0)
        return false
    }

    /**
     * 给编码出的aac裸流添加adts头字段
     * @param packet 要空出前7个字节，否则会搞乱数据
     * @param packetLen
     */
    private fun addADTStoPacket(packet: ByteArray, packetLen: Int)
    {
        val profile = 2  //AAC LC
        val freqIdx = 4  //44.1KHz
        val chanCfg = 2  //CPE
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }

    //TODO 定时调用，如果没有新数据，就用上一个数据
    @Throws(IOException::class)
    private fun videoStep(data: ByteArray?): Boolean
    {
        val index = mVideoEnc.dequeueInputBuffer(-1)
        if (index >= 0)
        {
            if (hasNewData)
            {

                yuv = ByteArray(width * height * 3 / 2)
                if (data != null && data.isNotEmpty())
                {
                    val flipData = flipData(data)
                    DataConvert.rgbaToYuv(flipData, width, height, yuv, convertType)
//                    rgbaToYuv(flipData, width, height, yuv)
//                    rgbaToYuv(data, width, height, yuv)
                }
            }
            val buffer = getInputBuffer(mVideoEnc, index)
            buffer?.clear()
            buffer?.put(yuv)
            mVideoEnc.queueInputBuffer(index, 0, yuv.size, (System.nanoTime() - nanoTime) / 1000, if (mStartFlag) 0 else MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        val mInfo = MediaCodec.BufferInfo()
        var outIndex = mVideoEnc.dequeueOutputBuffer(mInfo, 0)
        do
        {
            if (outIndex >= 0)
            {
                val outBuf = getOutputBuffer(mVideoEnc, outIndex)
                if (mAudioTrack >= 0 && mVideoTrack >= 0 && mInfo.size > 0 && mInfo.presentationTimeUs > 0)
                {
                    try
                    {
                        mMuxer.writeSampleData(mVideoTrack, outBuf!!, mInfo)
                    } catch (e: Exception)
                    {
                        Log.e(TAG, "video error:size=" + mInfo.size + "/offset="
                                + mInfo.offset + "/timeUs=" + mInfo.presentationTimeUs)
                        //e.printStackTrace();
                        Log.e(TAG, "-->" + e.message)
                    }

                }
                mVideoEnc.releaseOutputBuffer(outIndex, false)
                outIndex = mVideoEnc.dequeueOutputBuffer(mInfo, 0)
                if (mInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                {
                    Log.e(TAG, "video end")
                    return true
                }
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            {
                mVideoTrack = mMuxer.addTrack(mVideoEnc.outputFormat)
                Log.e(TAG, "add video track-->$mVideoTrack")
                if (mAudioTrack >= 0 && mVideoTrack >= 0)
                {
                    mMuxer.start()
                }
            }
        } while (outIndex >= 0)
        return false
    }


    private fun flipData(data: ByteArray): ByteArray
    {
        val flipData = ByteArray(data.size)
        for (i in 0 until height)
        {
            for (j in 0 until width * 4)
            {
                flipData[i * width * 4 + j] = data[(height - i - 1) * width * 4 + j]
            }

        }
        return flipData
    }


    private fun checkColorFormat(mime: String): Int
    {
        if (Build.MODEL == "HUAWEI P6-C00")
        {
            convertType = DataConvert.BGRA_YUV420SP
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        }
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
                                convertType = DataConvert.RGBA_YUV420P
                                return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                            } else if (c.colorFormats[j] == MediaCodecInfo.CodecCapabilities
                                            .COLOR_FormatYUV420SemiPlanar)
                            {
                                convertType = DataConvert.RGBA_YUV420SP
                                return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                            }
                        }
                    }
                }
            }
        }
        convertType = DataConvert.RGBA_YUV420SP
        return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    }


    companion object
    {

        val TAG = "RECORD"
    }

    fun feedData(data: ByteArray)
    {
        hasNewData = true
        nowFeedData = data
        //        nowTimeStep=timeStep;
    }

}
