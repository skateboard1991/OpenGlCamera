package com.skateboard.cameralib.widget

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.skateboard.cameralib.R
import com.skateboard.cameralib.filter.CameraFilter
import com.skateboard.cameralib.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraView(context: Context, attrs: AttributeSet?) : GLSurfaceView(context, attrs), GLSurfaceView.Renderer,SurfaceTexture.OnFrameAvailableListener
{


    companion object
    {
        val TAG = "CameraView"
    }

    private lateinit var surfaceTexture: SurfaceTexture

    private var cameraManager: CameraManager = CameraManager()

    private val vertexData = floatArrayOf(
            -1.0f,  1.0f,0.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,  1.0f,
            1.0f, 1.0f, 1.0f,  0.0f,
            1.0f,  -1.0f,1.0f, 1.0f)

    private var vPosition: Int = 0

    private var vCoord: Int = 0

    private var vMatrix: Int = 0

    private var matrix=FloatArray(16)

    private var vTexture: Int = 0

    private var program: Int = 0

    private var textureObj: Int = 0

    constructor(context: Context) : this(context, null)

    init
    {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    {
        textureObj=TextureUtil.createTextureObj()
        initSurfaceTexture(textureObj)
        val verShader=ShaderUtil.compileShader(GL_VERTEX_SHADER,SourceReaderUtil.readText(context,R.raw.ver_shader))
        val fragShader=ShaderUtil.compileShader(GL_FRAGMENT_SHADER,SourceReaderUtil.readText(context,R.raw.frag_shader))
        program=ProgramUtil.linkProgram(verShader, fragShader)
        glUseProgram(program)




        vTexture=glGetUniformLocation(program,"vTexture")
        glActiveTexture(GLES20.GL_TEXTURE0)
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureObj)
        glUniform1i(vTexture, 0)

        val data=GLDateUtil.generateFloatBuffer(vertexData)
        data.position(0)
        vPosition= glGetAttribLocation(program,"vPosition")
        glVertexAttribPointer(vPosition,2,GLES20.GL_FLOAT,false,4*4,data)
        glEnableVertexAttribArray(vPosition)

        data.position(2)
        vCoord= glGetAttribLocation(program,"vCoord")
        glVertexAttribPointer(vCoord,2,GLES20.GL_FLOAT,false,4*4,data)
        glEnableVertexAttribArray(vCoord)

        vMatrix=glGetUniformLocation(program,"vMatrix")
        glUniformMatrix4fv(vMatrix, 1, false, matrix, 0)




    }


    private fun initSurfaceTexture(textureObj:Int)
    {
        surfaceTexture= SurfaceTexture(textureObj)
        surfaceTexture.setOnFrameAvailableListener(this)
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?)
    {
        requestRender()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    {
        glViewport(0, 0, width, height)
        cameraManager.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        cameraManager.setPreviewTexture(surfaceTexture)
        cameraManager.startPreview()
    }



    override fun onDrawFrame(gl: GL10?)
    {
        surfaceTexture.updateTexImage()
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }



}