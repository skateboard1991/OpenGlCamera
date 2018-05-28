package com.skateboard.cameralib.widget

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Environment
import android.util.AttributeSet
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

    private var videoEncoder:TextureMovieEncoder

    init
    {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        videoEncoder=TextureMovieEncoder()
        cameraRender = CameraRender(this,videoEncoder)
        cameraRender.setOutputFile(generateFilePath())
    }


    fun generateFilePath():File
    {
        val dir= File(Environment.getExternalStorageDirectory(),"cameraTest")
        if(!dir.exists())
        {
            dir.mkdirs()
        }

        return File(dir.absolutePath, "test.mp4")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {
        cameraRender.onSurfaceCreated(gl, config)
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
        val isRecording=videoEncoder.isRecording
        cameraRender.changeRecordingState(!isRecording)

    }

    fun stopReceiveData()
    {
        val isRecording=videoEncoder.isRecording
        cameraRender.changeRecordingState(!isRecording)
    }


}