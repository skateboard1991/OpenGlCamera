package com.skateboard.cameralib.widget

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.*
import android.util.Log
import com.skateboard.cameralib.R
import com.skateboard.cameralib.codec.TextureMovieEncoder
import com.skateboard.cameralib.filter.CameraFilter
import com.skateboard.cameralib.filter.MaskFilter
import com.skateboard.cameralib.filter.ShowFilter
import com.skateboard.cameralib.util.*
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraRender(private val glSurfaceView: GLSurfaceView, private val mVideoEncoder: TextureMovieEncoder) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener
{

    companion object
    {
        val TAG = "CameraRender"

        private val RECORDING_OFF = 0
        private val RECORDING_ON = 1
        private val RECORDING_RESUMED = 2

    }

    private lateinit var surfaceTexture: SurfaceTexture

    private var cameraManager: CameraManager = CameraManager()

    private lateinit var cameraFilter: CameraFilter

    private lateinit var maskFilter: MaskFilter

    private lateinit var outputFilter: ShowFilter

    private var maskTextureId = 0

    private var textureId = 0

    private var ouputTextureId = 0

    private var screenWidth = 0

    private var screenHeight = 0

    private val bufferId = intArrayOf(0)

    private var rotateMatrix = FloatArray(16)

    private var recoverMatrix = FloatArray(16)

    private var mRecordingEnabled: Boolean = false

    private var mRecordingStatus: Int = 0

    private var outputFile: File? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {

        cameraFilter = CameraFilter(SourceReaderUtil.readText(glSurfaceView.context, R.raw.c_ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.c_frag_shader))
        maskFilter = MaskFilter(SourceReaderUtil.readText(glSurfaceView.context, R.raw.ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.frag_shader))
        outputFilter = ShowFilter(SourceReaderUtil.readText(glSurfaceView.context, R.raw.ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.frag_shader))

        Matrix.setIdentityM(rotateMatrix, 0)
        Matrix.rotateM(rotateMatrix, 0, 180f, 1f, 0f, 0f)

        Matrix.setIdentityM(recoverMatrix, 0)
        Matrix.rotateM(recoverMatrix, 0, -180f, 1f, 0f, 0f)

        GLES20.glGenFramebuffers(1, bufferId, 0)
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        maskTextureId = TextureUtil.createTextureObj(GLES20.GL_TEXTURE_2D)
        maskFilter.bindAttribute(maskTextureId)

        ouputTextureId = TextureUtil.createTextureObj(GLES20.GL_TEXTURE_2D)
        outputFilter.bindAttribute(ouputTextureId)

        textureId = TextureUtil.createTextureObj(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
        initSurfaceTexture(textureId)
        cameraFilter.bindAttribute(textureId)

        mRecordingStatus = if (mRecordingEnabled)
        {
            RECORDING_RESUMED
        } else
        {
            RECORDING_OFF
        }

    }

    fun takePicture(shutterCallback: Camera.ShutterCallback?,rawcallback:Camera.PictureCallback?,callback:Camera.PictureCallback?)
    {
        cameraManager.takePicture(shutterCallback,rawcallback,callback)
    }


    private fun initSurfaceTexture(textureId: Int)
    {
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setOnFrameAvailableListener(this)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    {
        GLES20.glViewport(0, 0, width, height)
        cameraManager.setSize(width, height)
        cameraManager.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        cameraManager.setBestDisplayOrientation(glSurfaceView.context, Camera.CameraInfo.CAMERA_FACING_BACK)
        cameraManager.setPreviewTexture(surfaceTexture)
        cameraManager.startPreview()
        screenWidth = width
        screenHeight = height
        outputFilter.setImage2D(width, height, GLES20.GL_RGBA)
    }

    fun startPreview()
    {
        cameraManager.startPreview()
    }

    fun stopPreveiw()
    {
        cameraManager.stopPreview()
    }

    fun setWaterMask(bitmap: Bitmap, x: Float, y: Float, width: Float, height: Float)
    {
        maskFilter.setMaskImg(bitmap, x, y, width, height)
    }

    fun setOutputFile(outputFile: File)
    {
        this.outputFile = outputFile
    }

    fun changeRecordingState(isRecording: Boolean)
    {
        Log.d(TAG, "changeRecordingState: was $mRecordingEnabled now $isRecording")
        mRecordingEnabled = isRecording
    }

    override fun onDrawFrame(gl: GL10?)
    {
        surfaceTexture.updateTexImage()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferId[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, ouputTextureId, 0)
        val statue = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (statue == GLES20.GL_FRAMEBUFFER_COMPLETE)
        {
            GLES20.glViewport(0, 0, screenWidth, screenHeight)
            cameraFilter.draw()
            maskFilter.draw()
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
        }
        clear(screenWidth, screenHeight)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ouputTextureId)
        outputFilter.draw()

        if (mRecordingEnabled)
        {
            when (mRecordingStatus)
            {
                RECORDING_OFF ->
                {
                    Log.d(TAG, "START recording")
                    // start recording
                    val file = this.outputFile
                    if (file != null)
                    {
                        mVideoEncoder.startRecording(TextureMovieEncoder.EncoderConfig(
                                file, screenWidth, screenHeight, 128000, EGL14.eglGetCurrentContext()))
                        mRecordingStatus = RECORDING_ON
                    }
                }
                RECORDING_RESUMED ->
                {
                    Log.d(TAG, "RESUME recording")
                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext())
                    mRecordingStatus = RECORDING_ON
                }
                RECORDING_ON ->
                {
                }
                else -> throw RuntimeException("unknown status $mRecordingStatus")
            } // yay
        } else
        {
            when (mRecordingStatus)
            {
                RECORDING_ON, RECORDING_RESUMED ->
                {
                    // stop recording
                    Log.d(TAG, "STOP recording")
                    mVideoEncoder.stopRecording()
                    mRecordingStatus = RECORDING_OFF
                    cameraManager.stopPreview()
                }
                RECORDING_OFF ->
                {
                }
                else -> throw RuntimeException("unknown status $mRecordingStatus")
            }
        }

        mVideoEncoder.setTextureId(ouputTextureId)

        mVideoEncoder.frameAvailable(surfaceTexture)

    }


    private fun clear(width: Int, height: Int)
    {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }


    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?)
    {
        glSurfaceView.requestRender()
    }

    fun releaseCamera()
    {
        cameraManager.release()
    }
}