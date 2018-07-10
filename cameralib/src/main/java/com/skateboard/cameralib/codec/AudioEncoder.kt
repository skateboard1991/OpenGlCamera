package com.skateboard.cameralib.codec


import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock

//class AudioEncoder : Runnable
//{
//    companion object
//    {
//        private val MSG_START_RECORDING = 0
//        private val MSG_STOP_RECORDING = 1
//        private val MSG_FRAME_AVAILABLE = 2
//        private val MSG_SET_TEXTURE_ID = 3
//        private val MSG_UPDATE_SHARED_CONTEXT = 4
//        private val MSG_QUIT = 5
//
//        private val TAG = "AudioEncoder"
//    }
//
//    private val lock = ReentrantLock()
//
//    private val condition = lock.newCondition()
//
//    private lateinit var encoderHandler: EncoderHandler
//
//    @Volatile
//    private var isReady = false
//
//
//    private class EncoderHandler(encoder: AudioEncoder) : Handler()
//    {
//        private val mWeakEncoder: WeakReference<AudioEncoder> = WeakReference(encoder)
//
//        override // runs on encoder thread
//        fun handleMessage(inputMessage: Message)
//        {
//            val what = inputMessage.what
//            val obj = inputMessage.obj
//
//            val encoder = mWeakEncoder.get()
//            if (encoder == null)
//            {
//                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null")
//                return
//            }
//
//            when (what)
//            {
//                MSG_START_RECORDING -> encoder.handleStartRecording(obj as Int)
//                MSG_STOP_RECORDING -> encoder.handleStopRecording()
//                MSG_FRAME_AVAILABLE ->
//                {
//                    val timestamp = inputMessage.arg1.toLong() shl 32 or (inputMessage.arg2.toLong() and 0xffffffffL)
//                    encoder.handleFrameAvailable(obj as FloatArray, timestamp)
//                }
//                MSG_QUIT -> Looper.myLooper()?.quit()
//                else -> throw RuntimeException("Unhandled msg what=$what")
//            }
//        }
//    }
//
//    private fun handleStartRecording(bitrate:Int)
//    {
//
//    }
//
//    fun startRecording(bitRate: Int)
//    {
//        Log.d(TAG, "Encoder: startRecording()")
////        synchronized(mReadyFence) {
////            if (mRunning)
////            {
////                Log.w(TAG, "Encoder thread already running")
////                return
////            }
////            mRunning = true
//        Thread(this, "TextureMovieEncoder").start()
//        while (!isReady)
//        {
//            try
//            {
//                condition.await()
//            } catch (ie: InterruptedException)
//            {
//                // ignore
//            }
//
//        }
////        }
//
//        encoderHandler.sendMessage(encoderHandler.obtainMessage(MSG_START_RECORDING, bitRate))
//    }
//
//
//    override fun run()
//    {
//        Looper.prepare()
//        lockWithAction {
//            encoderHandler = EncoderHandler(this)
//            isReady = true
//            condition.signalAll()
//        }
//        Looper.loop()
//        lockWithAction {
//
//            isReady = false
//        }
//
//    }
//
//
//    private fun lockWithAction(T: () -> Unit)
//    {
//        try
//        {
//            lock.lock()
//            T()
//        } catch (e: Exception)
//        {
//            e.printStackTrace()
//        } finally
//        {
//            lock.unlock()
//        }
//    }

//}