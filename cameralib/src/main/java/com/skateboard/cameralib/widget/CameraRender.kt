package com.skateboard.cameralib.widget

import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.skateboard.cameralib.R
import com.skateboard.cameralib.filter.CameraFilter
import com.skateboard.cameralib.filter.MaskFilter
import com.skateboard.cameralib.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRender(private val glSurfaceView: GLSurfaceView) : GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener
{

    companion object
    {
        val TAG = "CameraRender"
    }

    private lateinit var surfaceTexture: SurfaceTexture

    private var cameraManager: CameraManager = CameraManager()

    private var showFilter = CameraFilter()

    private var maskFilter = MaskFilter()


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {

        GLES20.glClearColor(1f, 1f, 1f, 1f)
        val maskTextureId=TextureUtil.createTextureObj(GLES20.GL_TEXTURE_2D)
        maskFilter.bindAttribute(SourceReaderUtil.readText(glSurfaceView.context, R.raw.ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.frag_shader), maskTextureId)
        maskFilter.setMaskImg(BitmapFactory.decodeResource(glSurfaceView.context.resources,R.drawable.ic_launcher),0,0)


        val textureId = TextureUtil.createTextureObj(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
        initSurfaceTexture(textureId)
        showFilter.bindAttribute(SourceReaderUtil.readText(glSurfaceView.context, R.raw.c_ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.c_frag_shader), textureId)
    }


    private fun initSurfaceTexture(textureId: Int)
    {
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setOnFrameAvailableListener(this)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    {
        GLES20.glViewport(0,0,width, height)
        cameraManager.setSize(width, height)
        cameraManager.open(Camera.CameraInfo.CAMERA_FACING_BACK)

        cameraManager.setBestDisplayOrientation(glSurfaceView.context,Camera.CameraInfo.CAMERA_FACING_BACK)
        cameraManager.setPreviewTexture(surfaceTexture)
        cameraManager.startPreview()
    }

    override fun onDrawFrame(gl: GL10?)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        surfaceTexture.updateTexImage()
        showFilter.draw()
        maskFilter.draw()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?)
    {
        glSurfaceView.requestRender()
    }
}