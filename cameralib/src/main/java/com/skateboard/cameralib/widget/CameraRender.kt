package com.skateboard.cameralib.widget

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLSurfaceView
import com.skateboard.cameralib.util.CameraManager
import com.skateboard.cameralib.util.LogUtil
import com.skateboard.cameralib.util.TextureUtil
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRender(private val context: Context) : GLSurfaceView.Renderer
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
        LogUtil.logW(TAG, "surfacecreated")
    }

    private fun initSurfaceTexture(textureObj: Int)
    {
        surfaceTexture = SurfaceTexture(textureObj)
        surfaceTexture.setOnFrameAvailableListener {

        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    {
        cameraManager.open(Camera.CameraInfo.CAMERA_FACING_BACK, width, height)
        cameraManager.setPreviewTexture(surfaceTexture)
        cameraManager.startPreview()
        LogUtil.logW(TAG, "surfacechanged")
    }

    override fun onDrawFrame(gl: GL10?)
    {
        if (surfaceTexture != null)
        {
            surfaceTexture.updateTexImage();
//            surfaceTexture.getTransformMatrix(transformMatrix)
        }
    }
}