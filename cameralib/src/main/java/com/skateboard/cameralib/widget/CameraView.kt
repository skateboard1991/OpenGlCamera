package com.skateboard.cameralib.widget

import android.content.Context

import android.opengl.GLSurfaceView
import android.util.AttributeSet
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraView(context: Context, attrs: AttributeSet?) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer
{


    companion object
    {
        val TAG = "CameraView"
    }

    constructor(context: Context) : this(context, null)

    private var cameraRender: CameraRender

    init
    {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        cameraRender = CameraRender(this)
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


}