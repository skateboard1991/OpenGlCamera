package com.skateboard.cameralib.codec

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGLContext
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.skateboard.cameralib.R
import com.skateboard.cameralib.egl.EglCore
import com.skateboard.cameralib.egl.WindowSurface
import com.skateboard.cameralib.filter.BaseFilter
import com.skateboard.cameralib.filter.ShowFilter
import com.skateboard.cameralib.util.ShaderUtil
import com.skateboard.cameralib.util.SourceReaderUtil
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference

class TextureMovieEncoder : Runnable
{

    // ----- accessed exclusively by encoder thread -----
    private var mInputWindowSurface: WindowSurface? = null
    private var mEglCore: EglCore? = null
    private var mFullScreen: BaseFilter? = null
    private var mTextureId: Int = 0
    private var mFrameNum: Int = 0
    private lateinit var mVideoEncoder: VideoEncoderCore

    // ----- accessed by multiple threads -----
    @Volatile
    private var mHandler: EncoderHandler? = null

    private val mReadyFence = Object()      // guards ready/running
    private var mReady: Boolean = false
    private var mRunning: Boolean = false

    /**
     * Returns true if recording has been started.
     */
    val isRecording: Boolean
        get() = synchronized(mReadyFence) {
            return mRunning
        }


    /**
     * Encoder configuration.
     *
     *
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     *
     *
     * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
     * with reasonable defaults for those and bit rate.
     */
    class EncoderConfig(internal val mOutputFile: File, internal val mWidth: Int, internal val mHeight: Int, internal val mBitRate: Int,
                        internal val mEglContext: EGLContext)
    {

        override fun toString(): String
        {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " to '" + mOutputFile.toString() + "' ctxt=" + mEglContext
        }
    }

    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     *
     *
     * Creates a new thread, which will create an encoder using the provided configuration.
     *
     *
     * Returns after the recorder thread has started and is ready to accept Messages.  The
     * encoder may not yet be fully configured.
     */
    fun startRecording(config: EncoderConfig)
    {
        Log.d(TAG, "Encoder: startRecording()")
        synchronized(mReadyFence) {
            if (mRunning)
            {
                Log.w(TAG, "Encoder thread already running")
                return
            }
            mRunning = true
            Thread(this, "TextureMovieEncoder").start()
            while (!mReady)
            {
                try
                {
                    mReadyFence.wait()
                } catch (ie: InterruptedException)
                {
                    // ignore
                }

            }
        }

        mHandler?.sendMessage(mHandler?.obtainMessage(MSG_START_RECORDING, config))
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     *
     *
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     *
     *
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    fun stopRecording()
    {
        mHandler?.sendMessage(mHandler?.obtainMessage(MSG_STOP_RECORDING))
        mHandler?.sendMessage(mHandler?.obtainMessage(MSG_QUIT))
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    fun updateSharedContext(sharedContext: EGLContext)
    {
        mHandler?.sendMessage(mHandler?.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext))
    }

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     *
     *
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     *
     *
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    fun frameAvailable(st: SurfaceTexture)
    {
        synchronized(mReadyFence) {
            if (!mReady)
            {
                return
            }
        }

        val transform = FloatArray(16)      // TODO - avoid alloc every frame
        Matrix.setIdentityM(transform,0)
        val timestamp = st.timestamp
        if (timestamp == 0L)
        {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            //
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero")
            return
        }

        mHandler?.sendMessage(mHandler?.obtainMessage(MSG_FRAME_AVAILABLE,
                (timestamp shr 32).toInt(), timestamp.toInt(), transform))
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     *
     *
     * TODO: do something less clumsy
     */
    fun setTextureId(id: Int)
    {
        synchronized(mReadyFence) {
            if (!mReady)
            {
                return
            }
        }
        mHandler?.sendMessage(mHandler?.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null))
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     *
     *
     * @see java.lang.Thread.run
     */
    override fun run()
    {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare()
        synchronized(mReadyFence) {
            mHandler = EncoderHandler(this)
            mReady = true
            mReadyFence.notify()
        }
        Looper.loop()

        Log.d(TAG, "Encoder thread exiting")
        synchronized(mReadyFence) {
            mRunning = false
            mReady = mRunning
            mHandler = null
        }
    }


    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private class EncoderHandler(encoder: TextureMovieEncoder) : Handler()
    {
        private val mWeakEncoder: WeakReference<TextureMovieEncoder>

        init
        {
            mWeakEncoder = WeakReference(encoder)
        }

        override// runs on encoder thread
        fun handleMessage(inputMessage: Message)
        {
            val what = inputMessage.what
            val obj = inputMessage.obj

            val encoder = mWeakEncoder.get()
            if (encoder == null)
            {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null")
                return
            }

            when (what)
            {
                MSG_START_RECORDING -> encoder.handleStartRecording(obj as EncoderConfig)
                MSG_STOP_RECORDING -> encoder.handleStopRecording()
                MSG_FRAME_AVAILABLE ->
                {
                    val timestamp = inputMessage.arg1.toLong() shl 32 or (inputMessage.arg2.toLong() and 0xffffffffL)
                    encoder.handleFrameAvailable(obj as FloatArray, timestamp)
                }
                MSG_SET_TEXTURE_ID -> encoder.handleSetTexture(inputMessage.arg1)
                MSG_UPDATE_SHARED_CONTEXT -> encoder.handleUpdateSharedContext(inputMessage.obj as EGLContext)
                MSG_QUIT -> Looper.myLooper()?.quit()
                else -> throw RuntimeException("Unhandled msg what=$what")
            }
        }
    }

    /**
     * Starts recording.
     */
    private fun handleStartRecording(config: EncoderConfig)
    {
        Log.d(TAG, "handleStartRecording $config")
        mFrameNum = 0
        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight, config.mBitRate,
                config.mOutputFile)
    }

    /**
     * Handles notification of an available frame.
     *
     *
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     *
     *
     * @param transform The texture transform, from SurfaceTexture.
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    private fun handleFrameAvailable(transform: FloatArray, timestampNanos: Long)
    {
        if (VERBOSE) Log.d(TAG, "handleFrameAvailable tr=$transform")
        mVideoEncoder.drainEncoder(false)
        mFullScreen?.draw()

//        drawBox(mFrameNum++)

        mInputWindowSurface?.setPresentationTime(timestampNanos)
        mInputWindowSurface?.swapBuffers()
    }

    /**
     * Handles a request to stop encoding.
     */
    private fun handleStopRecording()
    {
        Log.d(TAG, "handleStopRecording")
        mVideoEncoder?.drainEncoder(true)
        releaseEncoder()
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    private fun handleSetTexture(id: Int)
    {
        //Log.d(TAG, "handleSetTexture " + id);
        mTextureId = id
        mFullScreen?.bindAttribute(mTextureId)
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     *
     *
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private fun handleUpdateSharedContext(newSharedContext: EGLContext)
    {
        Log.d(TAG, "handleUpdatedSharedContext $newSharedContext")

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface?.releaseEglSurface()
        mFullScreen = null
        mEglCore?.release()

        // Create a new EGLContext and recreate the window surface.
        mEglCore = EglCore(newSharedContext, EglCore.FLAG_RECORDABLE)
        val eglCore = mEglCore
        if (eglCore != null)
        {
            mInputWindowSurface?.recreate(eglCore)
            mInputWindowSurface?.makeCurrent()
        }

        // Create new programs and such for the new context.
        mFullScreen = ShowFilter(ShaderUtil.VER_SHADER, ShaderUtil.FRAG_SHADER)
        mFullScreen?.bindAttribute(mTextureId)

    }

    private fun prepareEncoder(sharedContext: EGLContext, width: Int, height: Int, bitRate: Int,
                               outputFile: File)
    {
        try
        {
            mVideoEncoder = VideoEncoderCore(width, height, bitRate, outputFile)
        } catch (ioe: IOException)
        {
            throw RuntimeException(ioe)
        }

        mEglCore = EglCore(sharedContext, EglCore.FLAG_RECORDABLE)
        val eglCore = mEglCore
        if (eglCore != null)
        {
            mInputWindowSurface = WindowSurface(eglCore, mVideoEncoder.inputSurface, true)
            mInputWindowSurface?.makeCurrent()
        }
//        mFullScreen = FullFrameRect(
//                Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
        mFullScreen = ShowFilter(ShaderUtil.VER_SHADER, ShaderUtil.FRAG_SHADER)
        mFullScreen?.bindAttribute(mTextureId)
    }

    private fun releaseEncoder()
    {
        mVideoEncoder?.release()
        if (mInputWindowSurface != null)
        {
            mInputWindowSurface?.release()
            mInputWindowSurface = null
        }
        if (mFullScreen != null)
        {
//            mFullScreen?.release(false)
            mFullScreen = null
        }
        if (mEglCore != null)
        {
            mEglCore?.release()
            mEglCore = null
        }
    }

    /**
     * Draws a box, with position offset.
     */
    private fun drawBox(posn: Int)
    {
        val width = mInputWindowSurface?.width ?: 0
        val xpos = posn * 4 % (width - 50)
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
        GLES20.glScissor(xpos, 0, 100, 100)
        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
    }

    companion object
    {
        private val TAG = "TextureMovieEncoder"
        private val VERBOSE = false

        private val MSG_START_RECORDING = 0
        private val MSG_STOP_RECORDING = 1
        private val MSG_FRAME_AVAILABLE = 2
        private val MSG_SET_TEXTURE_ID = 3
        private val MSG_UPDATE_SHARED_CONTEXT = 4
        private val MSG_QUIT = 5
    }
}
