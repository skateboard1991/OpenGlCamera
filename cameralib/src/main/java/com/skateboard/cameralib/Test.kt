package com.skateboard.cameralib

/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Environment
import android.test.AndroidTestCase
import android.util.Log
import android.view.Surface

import java.io.File
import java.io.IOException

//20131106: removed hard-coded "/sdcard"
//20131205: added alpha to EGLConfig

/**
 * Generate an MP4 file using OpenGL ES drawing commands.  Demonstrates the use of MediaMuxer
 * and MediaCodec with Surface input.
 *
 *
 * This uses various features first available in Android "Jellybean" 4.3 (API 18).  There is
 * no equivalent functionality in previous releases.
 *
 *
 * (This was derived from bits and pieces of CTS tests, and is packaged as such, but is not
 * currently part of CTS.)
 */
class Test : AndroidTestCase()
{

    // size of a frame, in pixels
    private var mWidth = -1
    private var mHeight = -1
    // bit rate, in bits per second
    private var mBitRate = -1

    // encoder / muxer state
    private var mEncoder: MediaCodec? = null
    private var mInputSurface: CodecInputSurface? = null
    private var mMuxer: MediaMuxer? = null
    private var mTrackIndex: Int = 0
    private var mMuxerStarted: Boolean = false

    // allocate one of these up front so we don't need to do it every time
    private var mBufferInfo: MediaCodec.BufferInfo? = null


    /**
     * Tests encoding of AVC video from a Surface.  The output is saved as an MP4 file.
     */
    fun testEncodeVideoToMp4()
    {
        // QVGA at 2Mbps
        mWidth = 320
        mHeight = 240
        mBitRate = 2000000

        try
        {
            prepareEncoder()
            mInputSurface!!.makeCurrent()

            for (i in 0 until NUM_FRAMES)
            {
                // Feed any pending encoder output into the muxer.
                drainEncoder(false)

                // Generate a new frame of input.
                generateSurfaceFrame(i)
                mInputSurface!!.setPresentationTime(computePresentationTimeNsec(i))

                // Submit it to the encoder.  The eglSwapBuffers call will block if the input
                // is full, which would be bad if it stayed full until we dequeued an output
                // buffer (which we can't do, since we're stuck here).  So long as we fully drain
                // the encoder before supplying additional input, the system guarantees that we
                // can supply another frame without blocking.
                if (VERBOSE) Log.d(TAG, "sending frame $i to encoder")
                mInputSurface!!.swapBuffers()
            }

            // send end-of-stream to encoder, and drain remaining output
            drainEncoder(true)
        } finally
        {
            // release encoder, muxer, and input Surface
            releaseEncoder()
        }

        // To test the result, open the file with MediaExtractor, and get the format.  Pass
        // that into the MediaCodec decoder configuration, along with a SurfaceTexture surface,
        // and examine the output with glReadPixels.
    }

    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    private fun prepareEncoder()
    {
        mBufferInfo = MediaCodec.BufferInfo()

        val format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight)

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
        if (VERBOSE) Log.d(TAG, "format: $format")

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        //
        // If you want to have two EGL contexts -- one for display, one for recording --
        // you will likely want to defer instantiation of CodecInputSurface until after the
        // "display" EGL context is created, then modify the eglCreateContext call to
        // take eglGetCurrentContext() as the share_context argument.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE)
        mEncoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mInputSurface = CodecInputSurface(mEncoder!!.createInputSurface())
        mEncoder!!.start()

        // Output filename.  Ideally this would use Context.getFilesDir() rather than a
        // hard-coded output directory.
        val outputPath = File(OUTPUT_DIR,
                "test." + mWidth + "x" + mHeight + ".mp4").toString()
        Log.d(TAG, "output file is $outputPath")


        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        try
        {
            mMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (ioe: IOException)
        {
            throw RuntimeException("MediaMuxer creation failed", ioe)
        }

        mTrackIndex = -1
        mMuxerStarted = false
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    private fun releaseEncoder()
    {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects")
        if (mEncoder != null)
        {
            mEncoder!!.stop()
            mEncoder!!.release()
            mEncoder = null
        }
        if (mInputSurface != null)
        {
            mInputSurface!!.release()
            mInputSurface = null
        }
        if (mMuxer != null)
        {
            mMuxer!!.stop()
            mMuxer!!.release()
            mMuxer = null
        }
    }

    /**
     * Extracts all pending data from the encoder.
     *
     *
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     */
    private fun drainEncoder(endOfStream: Boolean)
    {
        val TIMEOUT_USEC = 10000
        if (VERBOSE) Log.d(TAG, "drainEncoder($endOfStream)")

        if (endOfStream)
        {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder")
            mEncoder!!.signalEndOfInputStream()
        }

        var encoderOutputBuffers = mEncoder!!.outputBuffers
        while (true)
        {
            val encoderStatus = mEncoder!!.dequeueOutputBuffer(mBufferInfo!!, TIMEOUT_USEC.toLong())
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER)
            {
                // no output available yet
                if (!endOfStream)
                {
                    break      // out of while
                } else
                {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS")
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)
            {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder!!.outputBuffers
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted)
                {
                    throw RuntimeException("format changed twice")
                }
                val newFormat = mEncoder!!.outputFormat
                Log.d(TAG, "encoder output format changed: $newFormat")

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer!!.addTrack(newFormat)
                mMuxer!!.start()
                mMuxerStarted = true
            } else if (encoderStatus < 0)
            {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
                // let's ignore it
            } else
            {
                val encodedData = encoderOutputBuffers[encoderStatus]
                        ?: throw RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null")

                if (mBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0)
                {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
                    mBufferInfo!!.size = 0
                }

                if (mBufferInfo!!.size != 0)
                {
                    if (!mMuxerStarted)
                    {
                        throw RuntimeException("muxer hasn't started")
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo!!.offset)
                    encodedData.limit(mBufferInfo!!.offset + mBufferInfo!!.size)

                    mMuxer!!.writeSampleData(mTrackIndex, encodedData, mBufferInfo!!)
                    if (VERBOSE) Log.d(TAG, "sent " + mBufferInfo!!.size + " bytes to muxer")
                }

                mEncoder!!.releaseOutputBuffer(encoderStatus, false)

                if (mBufferInfo!!.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                {
                    if (!endOfStream)
                    {
                        Log.w(TAG, "reached end of stream unexpectedly")
                    } else
                    {
                        if (VERBOSE) Log.d(TAG, "end of stream reached")
                    }
                    break      // out of while
                }
            }
        }
    }

    /**
     * Generates a frame of data using GL commands.  We have an 8-frame animation
     * sequence that wraps around.  It looks like this:
     * <pre>
     * 0 1 2 3
     * 7 6 5 4
    </pre> *
     * We draw one of the eight rectangles and leave the rest set to the clear color.
     */
    private fun generateSurfaceFrame(frameIndex: Int)
    {
        var frameIndex = frameIndex
        frameIndex %= 8

        val startX: Int
        val startY: Int
        if (frameIndex < 4)
        {
            // (0,0) is bottom-left in GL
            startX = frameIndex * (mWidth / 4)
            startY = mHeight / 2
        } else
        {
            startX = (7 - frameIndex) * (mWidth / 4)
            startY = 0
        }

        GLES20.glClearColor(TEST_R0 / 255.0f, TEST_G0 / 255.0f, TEST_B0 / 255.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
        GLES20.glScissor(startX, startY, mWidth / 4, mHeight / 2)
        GLES20.glClearColor(TEST_R1 / 255.0f, TEST_G1 / 255.0f, TEST_B1 / 255.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
    }


    /**
     * Holds state associated with a Surface used for MediaCodec encoder input.
     *
     *
     * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
     * to create an EGL window surface.  Calls to eglSwapBuffers() cause a frame of data to be sent
     * to the video encoder.
     *
     *
     * This object owns the Surface -- releasing this will release the Surface too.
     */
    private class CodecInputSurface
    /**
     * Creates a CodecInputSurface from a Surface.
     */
    (private var mSurface: Surface?)
    {

        private var mEGLDisplay = EGL14.EGL_NO_DISPLAY
        private var mEGLContext = EGL14.EGL_NO_CONTEXT
        private var mEGLSurface = EGL14.EGL_NO_SURFACE

        init
        {
            if (mSurface == null)
            {
                throw NullPointerException()
            }

            eglSetup()
        }

        /**
         * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
         */
        private fun eglSetup()
        {
            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (mEGLDisplay === EGL14.EGL_NO_DISPLAY)
            {
                throw RuntimeException("unable to get EGL14 display")
            }
            val version = IntArray(2)
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1))
            {
                throw RuntimeException("unable to initialize EGL14")
            }

            // Configure EGL for recording and OpenGL ES 2.0.
            val attribList = intArrayOf(EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL_RECORDABLE_ANDROID, 1, EGL14.EGL_NONE)
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.size,
                    numConfigs, 0)
            checkEglError("eglCreateContext RGB888+recordable ES2")

            // Configure context for OpenGL ES 2.0.
            val attrib_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                    attrib_list, 0)
            checkEglError("eglCreateContext")

            // Create a window surface, and attach it to the Surface we received.
            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                    surfaceAttribs, 0)
            checkEglError("eglCreateWindowSurface")
        }

        /**
         * Discards all resources held by this class, notably the EGL context.  Also releases the
         * Surface that was passed to our constructor.
         */
        fun release()
        {
            if (mEGLDisplay !== EGL14.EGL_NO_DISPLAY)
            {
                EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
                EGL14.eglReleaseThread()
                EGL14.eglTerminate(mEGLDisplay)
            }

            mSurface!!.release()

            mEGLDisplay = EGL14.EGL_NO_DISPLAY
            mEGLContext = EGL14.EGL_NO_CONTEXT
            mEGLSurface = EGL14.EGL_NO_SURFACE

            mSurface = null
        }

        /**
         * Makes our EGL context and surface current.
         */
        fun makeCurrent()
        {
            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)
            checkEglError("eglMakeCurrent")
        }

        /**
         * Calls eglSwapBuffers.  Use this to "publish" the current frame.
         */
        fun swapBuffers(): Boolean
        {
            val result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface)
            checkEglError("eglSwapBuffers")
            return result
        }

        /**
         * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
         */
        fun setPresentationTime(nsecs: Long)
        {
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs)
            checkEglError("eglPresentationTimeANDROID")
        }

        /**
         * Checks for EGL errors.  Throws an exception if one is found.
         */
        private fun checkEglError(msg: String)
        {
            val error: Int=EGL14.eglGetError()
            if (error != EGL14.EGL_SUCCESS)
            {
                throw RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error))
            }
        }

        companion object
        {
            private val EGL_RECORDABLE_ANDROID = 0x3142
        }
    }

    companion object
    {
        private val TAG = "EncodeAndMuxTest"
        private val VERBOSE = false           // lots of logging

        // where to put the output file (note: /sdcard requires WRITE_EXTERNAL_STORAGE permission)
        private val OUTPUT_DIR = Environment.getExternalStorageDirectory()

        // parameters for the encoder
        private val MIME_TYPE = "video/avc"    // H.264 Advanced Video Coding
        private val FRAME_RATE = 15               // 15fps
        private val IFRAME_INTERVAL = 10          // 10 seconds between I-frames
        private val NUM_FRAMES = 30               // two seconds of video

        // RGB color values for generated frames
        private val TEST_R0 = 0
        private val TEST_G0 = 136
        private val TEST_B0 = 0
        private val TEST_R1 = 236
        private val TEST_G1 = 50
        private val TEST_B1 = 186

        /**
         * Generates the presentation time for frame N, in nanoseconds.
         */
        private fun computePresentationTimeNsec(frameIndex: Int): Long
        {
            val ONE_BILLION: Long = 1000000000
            return frameIndex * ONE_BILLION / FRAME_RATE
        }
    }
}
