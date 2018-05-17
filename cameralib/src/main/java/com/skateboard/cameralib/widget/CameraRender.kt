package com.skateboard.cameralib.widget

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLSurfaceView
import com.skateboard.cameralib.util.CameraManager
import com.skateboard.cameralib.util.TextureUtil
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

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {
        val textureObj = TextureUtil.createTextureObj()
        initSurfaceTexture(textureObj)
    }

    private fun initSurfaceTexture(textureObj: Int)
    {
        surfaceTexture = SurfaceTexture(textureObj)
        surfaceTexture.setOnFrameAvailableListener(this)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    {
        cameraManager.setSize(width, height)
        cameraManager.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        cameraManager.setPreviewTexture(surfaceTexture)
        cameraManager.startPreview()
    }

    override fun onDrawFrame(gl: GL10?)
    {
        surfaceTexture.updateTexImage()

    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?)
    {
        glSurfaceView.requestRender()
    }
}