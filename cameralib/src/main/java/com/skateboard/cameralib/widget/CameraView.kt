package com.skateboard.cameralib.widget

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.SurfaceHolder
import com.skateboard.cameralib.codec.TextureMovieEncoder
import kotlinx.android.synthetic.main.activity_record.view.*
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraView(context: Context, attrs: AttributeSet?) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer
{


    constructor(context: Context) : this(context, null)

    private var cameraRender: CameraRender

    private var videoEncoder: TextureMovieEncoder


    private var waterBitmap: Bitmap? = null

    private var waterX = -1f

    private var waterY = 1f

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

    fun setWaterMask(bitmap: Bitmap, waterX: Float, waterY: Float)
    {
        this.waterBitmap = bitmap
        this.waterX = waterX
        this.waterY = waterY
    }


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {
        cameraRender.onSurfaceCreated(gl, config)

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    {
        cameraRender.onSurfaceChanged(gl, width, height)
        val bitmap = waterBitmap
        if (bitmap != null)
        {
            cameraRender.setWaterMask(bitmap, waterX, waterY, width.toFloat(), height.toFloat())
        }
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

    fun startPreview()
    {
        cameraRender.startPreview()
    }

    fun takePicture(shutterCallback: Camera.ShutterCallback?,rawcallback:Camera.PictureCallback?,callback:Camera.PictureCallback?)
    {
        cameraRender.takePicture(shutterCallback,rawcallback,callback)
    }

    fun stopPreview()
    {
        cameraRender.stopPreveiw()
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
        cameraRender.releaseCamera()
        super.surfaceDestroyed(holder)
    }
}