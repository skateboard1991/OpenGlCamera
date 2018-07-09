package com.skateboard.cameralib.codec

import android.media.*
import android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM
import android.os.Build
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class AudioEncoderCore(private val mediaMuxerWrapper: MediaMuxerWrapper)
{
    private lateinit var audioCodec: MediaCodec

    private lateinit var bufferInfo: MediaCodec.BufferInfo

    private var trackIndex = -1

    private var isEncoding = false

    private var lastPreTime = -1L

    private var audioRecord: AudioRecord? = null

    private var minBufferSize = 9000

    private val sampleRateSize = 44100

    private val executors = Executors.newSingleThreadExecutor()

    fun prepare(bitrate: Int)
    {
        prepareAudioRecord()
        prepareAudioCodec(bitrate, sampleRateSize)
    }

    private fun prepareAudioRecord()
    {
        minBufferSize = AudioRecord.getMinBufferSize(sampleRateSize, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateSize, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize)
        audioRecord?.startRecording()
    }


    private fun prepareAudioCodec(bitrate: Int, sampleRate: Int)
    {
        bufferInfo = MediaCodec.BufferInfo()
        val mediaFormat = MediaFormat()
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize)
        audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        audioCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioCodec.start()
    }


    private val recordRunnable = Runnable {

        while (isEncoding)
        {
            val data = ByteArray(minBufferSize)
            audioRecord?.read(data, 0, data.size)
            drainEncoder(data,false)
        }
        drainEncoder(null,true)
        audioRecord?.stop()
        audioRecord?.release()
    }

    fun startRecord()
    {
        if (!isEncoding)
        {
            isEncoding = true
            executors.execute(recordRunnable)
            executors.shutdown()
        }
    }

    fun drainEncoder(data: ByteArray?, isEOS: Boolean)
    {

        while (true)
        {
            val inIndex = audioCodec.dequeueInputBuffer(0)
            if (inIndex >= 0)
            {
                if (isEOS)
                {
                    audioCodec.queueInputBuffer(inIndex, 0, 0, System.nanoTime() / 1000, BUFFER_FLAG_END_OF_STREAM)
                }
                else
                {
                    val inBuffer = getInBuffer(inIndex)
                    inBuffer.clear()
                    inBuffer.put(data)
                    audioCodec.queueInputBuffer(inIndex, 0, data?.size?:0, System.nanoTime() / 1000, 0)
                }

            }
            else if (inIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
            {


            }


            var outIndex = audioCodec.dequeueOutputBuffer(bufferInfo, 1000L)
            while (outIndex >= 0)
            {

                if (mediaMuxerWrapper.isStarting())
                {
                    if (lastPreTime == -1L)
                    {
                        lastPreTime = bufferInfo.presentationTimeUs
                    }
                    else if (lastPreTime < bufferInfo.presentationTimeUs)
                    {
                        lastPreTime = bufferInfo.presentationTimeUs
                    }
                    if (bufferInfo.size != 0 && lastPreTime <= bufferInfo.presentationTimeUs)
                    {
                        val outBuffer = getOutBuffer(outIndex)
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mediaMuxerWrapper.writeSampleData(trackIndex, outBuffer, bufferInfo)
                    }
                    audioCodec.releaseOutputBuffer(outIndex, false)
                    outIndex = audioCodec.dequeueOutputBuffer(bufferInfo, 0)
                }
                else
                {
                    audioCodec.releaseOutputBuffer(outIndex, false)
                    break
                }
            }

            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            {
                trackIndex = mediaMuxerWrapper.addTrack(audioCodec.outputFormat)
                mediaMuxerWrapper.start()
            }
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
            {
                if (!isEOS)
                {
                    break
                }
            }

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
            {

                isEncoding = false

                break
            }


        }

    }

    fun release()
    {
        isEncoding = false
        audioRecord = null
        audioCodec.stop()
        audioCodec.release()
        mediaMuxerWrapper.stop()
        mediaMuxerWrapper.release()
    }


    private fun getInBuffer(index: Int): ByteBuffer
    {
        return if (Build.VERSION.SDK_INT >= 21)
        {
            audioCodec.getInputBuffer(index)
        }
        else
        {
            audioCodec.inputBuffers[index]
        }
    }

    private fun getOutBuffer(index: Int): ByteBuffer
    {
        return if (Build.VERSION.SDK_INT >= 21)
        {
            audioCodec.getOutputBuffer(index)
        }
        else
        {
            audioCodec.outputBuffers[index]
        }
    }
}