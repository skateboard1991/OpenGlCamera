package com.skateboard.cameralib.codec

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.util.concurrent.Executors

class AudioRecorder(private val mediaMuxerWrapper: MediaMuxerWrapper)
{
    private var audioRecord: AudioRecord? = null

    private lateinit var outputFile: File

    private val DIR = "recordDemo"

    private val FILE_NAME = "audio.mp4"

    private var isRecording = false

    private var minSize = 1012

    private lateinit var audioEncoder: AudioEncoderCore

    private var executorService = Executors.newSingleThreadExecutor()

    fun prepare(audioSource: Int = MediaRecorder.AudioSource.MIC, sampleRateInHz: Int = 44100, channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO, audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes: Int = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat))
    {

        prepareAudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)

        prepareAudioEncoder(sampleRateInHz)
    }


    private fun prepareAudioRecord(audioSource: Int, sampleRateInHz: Int, channelConfig: Int, audioFormat: Int, bufferSizeInBytes: Int)
    {
        minSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)

        audioRecord = AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)
    }


    private fun prepareAudioEncoder(sampleRate: Int)
    {
        audioEncoder = AudioEncoderCore(mediaMuxerWrapper)
        audioEncoder.prepare(128000, sampleRate)

    }


    private val recordRunnable = Runnable {


        val data = ByteArray(minSize)
        audioRecord?.startRecording()
        while (isRecording)
        {
            audioRecord?.read(data, 0, data.size)
            audioEncoder.start()
            audioEncoder.drainEncoder(data)
        }
        audioEncoder.release()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }


    fun startRecord()
    {
        if (!isRecording)
        {
            isRecording = true
            executorService.execute(recordRunnable)
        }
    }

    fun stopRecord()
    {
        isRecording = false
    }

    fun destroy()
    {
        executorService.shutdown()
    }

}