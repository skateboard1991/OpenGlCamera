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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
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

    private var previewWidth = 0

    private var previewHeight = 0

    private var isKeepCallback = false

    private lateinit var outPutBuffer: ByteBuffer

    private var maskTextureId = 0

    private var textureId = 0

    private var update = true

    var frameCallback: CameraView.OnFrameCallback? = null

    private val bufferId = intArrayOf(0)

    private val cameraByteQueue: Queue<ByteArray> = LinkedBlockingQueue<ByteArray>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {
        GLES20.glGenFramebuffers(1, bufferId, 0)
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        maskTextureId = TextureUtil.createTextureObj(GLES20.GL_TEXTURE_2D)
        maskFilter.bindAttribute(SourceReaderUtil.readText(glSurfaceView.context, R.raw.ver_shader), SourceReaderUtil.readText(glSurfaceView.context, R.raw.frag_shader), maskTextureId)
        maskFilter.setMaskImg(BitmapFactory.decodeResource(glSurfaceView.context.resources, R.drawable.ic_launcher), 0, 0)


        textureId = TextureUtil.createTextureObj(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
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
        GLES20.glViewport(0, 0, width, height)
        cameraManager.setSize(width, height)
        cameraManager.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        cameraManager.setBestDisplayOrientation(glSurfaceView.context, Camera.CameraInfo.CAMERA_FACING_BACK)
        cameraManager.setPreviewTexture(surfaceTexture)
        cameraManager.camera?.setPreviewCallbackWithBuffer { data, camera ->

            cameraManager.camera?.addCallbackBuffer(data)

        }
        cameraManager.startPreview()
        previewWidth = 720
        previewHeight = 1280
        frameCallback?.onPreviewSizeChanged(previewWidth, previewHeight)
        outPutBuffer = ByteBuffer.allocate(previewWidth * previewHeight * 4)
        outPutBuffer.position(0)
    }


    fun update(bytes: ByteArray)
    {
        cameraByteQueue.add(bytes)
        this.update = true
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
//        if (update)
//        {
//            update = false
            surfaceTexture.updateTexImage()
//        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bufferId[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, maskTextureId, 0)
        val statue = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (statue == GLES20.GL_FRAMEBUFFER_COMPLETE)
        {
            maskFilter.draw()
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_NONE)
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        showFilter.draw()
        maskFilter.draw()
        if (isKeepCallback)
        {
            GLES20.glReadPixels(0, 0, 720, 1280, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, outPutBuffer)
            frameCallback?.onFrameBack(outPutBuffer.array())
        }

        val d = cameraByteQueue.poll()
        cameraManager.camera?.addCallbackBuffer(d)

    }


    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?)
    {
        glSurfaceView.requestRender()
    }
}