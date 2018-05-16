package com.skateboard.cameralib.widget

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.AttributeSet
import com.skateboard.cameralib.R
import com.skateboard.cameralib.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraView(context: Context, attrs: AttributeSet?) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer
{

    companion object
    {
        val TAG = "CameraView"
    }

    private lateinit var surfaceTexture: SurfaceTexture

    private var cameraManager: CameraManager = CameraManager()

    private val vertexData = floatArrayOf(
            1f,  1f,  1f,  1f,
            -1f,  1f,  0f,  1f,
            -1f, -1f,  0f,  0f,
            1f,  1f,  1f,  1f,
            -1f, -1f,  0f,  0f,
            1f, -1f,  1f,  0f)

    private var aPosition: Int = 0

    private var aCoord: Int = 0

    private var uMatrix: Int = 0

    private var matrix=FloatArray(16)

    private var vTexture: Int = 0

    private var program: Int = 0

    private var textureObj: Int = 0

    constructor(context: Context) : this(context, null)

    init
    {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode=GLSurfaceView.RENDERMODE_WHEN_DIRTY
        preserveEGLContextOnPause = true
        cameraDistance = 100f
    }



    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {
        glClearColor(0f, 0f, 0f, 0f)
        textureObj = TextureUtil.createTextureObj()
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureObj)
        initSurfaceTexture(textureObj)
        LogUtil.logW(CameraRender.TAG, "surfacecreated")
        val verShader = ShaderUtil.compileShader(GLES20.GL_VERTEX_SHADER, SourceReaderUtil.readText(context, R.raw.ver_shader))
        val fragShader = ShaderUtil.compileShader(GLES20.GL_FRAGMENT_SHADER, SourceReaderUtil.readText(context, R.raw.frag_shader))
        program = ProgramUtil.linkProgram(verShader, fragShader)
        ProgramUtil.useProgram(program)
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        val verData = GLDateUtil.generateFloatBuffer(vertexData)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 4 * 4, verData)
        GLES20.glEnableVertexAttribArray(aPosition)
        aCoord = GLES20.glGetAttribLocation(program, "aCoord")
        verData.position(2)
        GLES20.glVertexAttribPointer(aCoord, 2, GLES20.GL_FLOAT, false, 4 * 4, verData)
        GLES20.glEnableVertexAttribArray(aCoord)
        uMatrix = GLES20.glGetUniformLocation(program, "uMatrix")
        vTexture = GLES20.glGetUniformLocation(program, "vTexture")


    }

    private fun initSurfaceTexture(textureObj: Int)
    {
        surfaceTexture = SurfaceTexture(textureObj)
        surfaceTexture.setOnFrameAvailableListener {

            this@CameraView.requestRender()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    {
        glViewport(0, 0, width, height)
        LogUtil.logW(CameraRender.TAG, "surfacechanged")
        cameraManager.open(Camera.CameraInfo.CAMERA_FACING_BACK, width, height)
        cameraManager.setPreviewTexture(surfaceTexture)
        cameraManager.startPreview()
    }

    override fun onDrawFrame(gl: GL10?)
    {
        glClear(GLES20.GL_COLOR_BUFFER_BIT)
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(matrix)
        glUniformMatrix4fv(uMatrix, 1, false, matrix, 0)
        glActiveTexture(GLES20.GL_TEXTURE0)
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureObj)
        glUniform1i(vTexture, 0)
        glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
    }


}