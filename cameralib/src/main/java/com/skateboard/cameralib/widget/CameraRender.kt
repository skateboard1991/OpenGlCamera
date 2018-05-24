package com.skateboard.cameralib.widget

import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.skateboard.cameralib.R
import com.skateboard.cameralib.filter.CameraFilter
import com.skateboard.cameralib.filter.MaskFilter
import com.skateboard.cameralib.filter.ShowFilter
import com.skateboard.cameralib.util.*
import java.nio.ByteBuffer
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

    private var cameraFilter = CameraFilter()

    private var maskFilter = MaskFilter()

    private var outputFilter = ShowFilter()

    private var trackFilter = ShowFilter()

    private var previewWidth = 0

    private var previewHeight = 0

    private var isKeepCallback = false

    private lateinit var outPutBuffer: ByteBuffer

    private var maskTextureId = 0

    private var textureId = 0

    private var trackTextureId = 0

    private var ouputTextureId = 0

    private var screenWidth = 0

    private var screenHeight = 0

    var frameCallback: CameraView.OnFrameCallback? = null

    private val bufferId = intArrayOf(0, 0)

    private var rotateMatrix = FloatArray(16)

    private var recoverMatrix = FloatArray(16)

    private val framebackThread = Thread {

        while (isKeepCallback)
        {
            GLES20.glReadPixels(0, 0, previewWidth, previewHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, outPutBuffer)
            frameCallback?.onFrameBack(outPutBuffer.array())
        }

    }


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {
        Matrix.setIdentityM(rotateMatrix, 0)
        Matrix.rotateM(rotateMatrix, 0, 180f, 1f, 0f, 0f)

        Matrix.setIdentityM(recoverMatrix, 0)
        Matrix.rotateM(recoverMatrix, 0, -180f, 1f, 0f, 0f)

        GLES20.glGenFramebuffers(2, bufferId, 0)
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        maskTextureId = TextureUtil.createTextureObj(GLES20.GL_TEXTURE_2D)
        maskFilter.bindAttribute(SourceReaderUtil.readText(glSurfaceView.context, R.raw.ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.frag_shader), maskTextureId)
        maskFilter.setMaskImg(BitmapFactory.decodeResource(glSurfaceView.context.resources, R.drawable.ic_launcher), 0, 0)

        ouputTextureId = TextureUtil.createTextureObj(GLES20.GL_TEXTURE_2D)
        outputFilter.bindAttribute(SourceReaderUtil.readText(glSurfaceView.context, R.raw.ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.frag_shader), ouputTextureId)

        trackTextureId = TextureUtil.createTextureObj(GLES20.GL_TEXTURE_2D)
        trackFilter.bindAttribute(SourceReaderUtil.readText(glSurfaceView.context, R.raw.ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.frag_shader), trackTextureId)

        textureId = TextureUtil.createTextureObj(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
        initSurfaceTexture(textureId)
        cameraFilter.bindAttribute(SourceReaderUtil.readText(glSurfaceView.context, R.raw.c_ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.c_frag_shader), textureId)

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
        previewWidth = 720
        previewHeight = 1280
        cameraManager.camera?.setPreviewCallback { data, camera ->

            cameraManager.camera?.addCallbackBuffer(data)
        }

        cameraManager.camera?.addCallbackBuffer(ByteArray(previewWidth * previewHeight * 4))
        cameraManager.startPreview()
        screenWidth = width
        screenHeight = height
        frameCallback?.onPreviewSizeChanged(previewWidth, previewHeight)
        outPutBuffer = ByteBuffer.allocate(previewWidth * previewHeight * 4)
        outPutBuffer.position(0)
        outputFilter.setImage2D(width, height, GLES20.GL_RGBA)
        trackFilter.setImage2D(width, height, GLES20.GL_RGBA)
    }


    fun startReceiveData()
    {
        isKeepCallback = true
    }

    fun stopReceiveData()
    {
        isKeepCallback = false
    }

    override fun onDrawFrame(gl: GL10?)
    {
        surfaceTexture.updateTexImage()
        clear(screenWidth, screenHeight)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferId[1])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, ouputTextureId, 0)
        var statue = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (statue == GLES20.GL_FRAMEBUFFER_COMPLETE)
        {
            clear(screenWidth, screenHeight)
            Matrix.setIdentityM(maskFilter.matrix, 0)
            cameraFilter.draw()
            maskFilter.draw()
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
        }
        outputFilter.draw()

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferId[1])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, trackTextureId, 0)
        statue = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (statue == GLES20.GL_FRAMEBUFFER_COMPLETE)
        {
            clear(previewWidth, previewHeight)
            outputFilter.draw()
            if (isKeepCallback && !framebackThread.isAlive)
            {
                framebackThread.start()
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)

        }

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
}