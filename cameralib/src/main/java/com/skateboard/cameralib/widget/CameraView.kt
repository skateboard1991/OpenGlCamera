package com.skateboard.cameralib.widget

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.SurfaceHolder
import com.skateboard.cameralib.codec.TextureMovieEncoder
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraView(context: Context, attrs: AttributeSet?) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer
{


    companion object
    {
        val TAG = "CameraView"

        private val sVideoEncoder = TextureMovieEncoder()
    }

    constructor(context: Context) : this(context, null)

    private var cameraRender: CameraRender

    private var videoEncoder: TextureMovieEncoder


    private var waterBitmap: Bitmap? = null

    init
    {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        videoEncoder = TextureMovieEncoder()
        cameraRender = CameraRender(this, videoEncoder)
    }

    fun setOutputFile(outputFile: File)
    {
        queueEvent {
            cameraRender.setOutputFile(outputFile)
        }

    }

    fun setWaterMask(bitmap: Bitmap)
    {
        waterBitmap = bitmap
    }


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {
        cameraRender.onSurfaceCreated(gl, config)
        val bitmap = waterBitmap
        if (bitmap != null)
        {
            cameraRender.setWaterMask(bitmap)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    {
        cameraRender.onSurfaceChanged(gl, width, height)
    }


    override fun onDrawFrame(gl: GL10?)
    {
        cameraRender.onDrawFrame(gl)
    }


    fun startReceiveData()
    {
        val isRecording = videoEncoder.isRecording
        changeRecordingState(!isRecording)

    }

    fun stopReceiveData()
    {
        val isRecording = videoEncoder.isRecording
        changeRecordingState(!isRecording)
    }

    private fun changeRecordingState(isRecording: Boolean)
    {
        cameraRender.changeRecordingState(isRecording)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?)
    {
        super.surfaceDestroyed(holder)
        cameraRender.releaseCamera()
    }
}