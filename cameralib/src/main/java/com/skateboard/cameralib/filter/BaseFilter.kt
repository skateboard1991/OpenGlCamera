package com.skateboard.cameralib.filter

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.Matrix
import com.skateboard.cameralib.util.ProgramUtil
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class BaseFilter
{
    protected var vPosition = 0

    protected var vCoord = 0

    protected var program = 0

    protected var vTexture = 0

    protected var textureId = 0

    protected var vMatrix = 0

    var matrix=FloatArray(16)

    open var verData = floatArrayOf(

//            -1f,1f,0f,1f,
//            -1f,-1f,1f,0f,
//            1f,1f,0f,0f,
//            1f,-1f,1f,0f
//
            1f, 1f, 1f, 1f,
            -1f, 1f, 0f, 1f,
            -1f, -1f, 0f, 0f,
            1f, 1f, 1f, 1f,
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f
    )

    val FLOAT_SIZE = 4

    val POSITION_SIZE = 2

    protected lateinit var verBuffer:Buffer


    open fun bindAttribute(verSource: String, fragSource: String, textureId: Int)
    {
        Matrix.setIdentityM(matrix,0)
        program = ProgramUtil.linkProgram(verSource, fragSource)
        vPosition = glGetAttribLocation(program, "vPosition")
        glEnableVertexAttribArray(vPosition)
        vCoord = glGetAttribLocation(program, "vCoord")
        glEnableVertexAttribArray(vCoord)
        vTexture = glGetUniformLocation(program, "vTexture")
        this.textureId = textureId
        vMatrix = glGetUniformLocation(program,"vMatrix")
        verBuffer = generateVerBuffer(verData)
//
    }

    protected fun generateVerBuffer(floatArray: FloatArray): Buffer
    {

        val verBuffer = ByteBuffer.allocateDirect(floatArray.size * FLOAT_SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        verBuffer.put(floatArray)
        verBuffer.position(0)
        return verBuffer
    }

    open fun draw()
    {
        glUseProgram(program)
        onVertexAttribPointer()
        onBindTexture()
        onDraw()
    }


    open protected fun onVertexAttribPointer()
    {
        verBuffer.position(0)
        glVertexAttribPointer(vPosition, POSITION_SIZE, GL_FLOAT, false, FLOAT_SIZE * 4, verBuffer)
        verBuffer.position(2)
        glVertexAttribPointer(vCoord, POSITION_SIZE, GL_FLOAT, false, FLOAT_SIZE * 4, verBuffer)
        glUniformMatrix4fv(vMatrix,1,false,matrix,0)
    }


    open protected fun onBindTexture()
    {
        glBindTexture(GLES20.GL_TEXTURE_2D,textureId)
    }

    open protected fun onDraw()
    {

        glDrawArrays(GL_TRIANGLES, 0, 6)

    }

}