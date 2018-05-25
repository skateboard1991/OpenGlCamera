package com.skateboard.cameralib.filter

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.Matrix
import com.skateboard.cameralib.util.ProgramUtil
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

open class BaseFilter(verSource: String, fragSource: String)
{


    protected var vPosition = 0

    protected var vCoord = 0

    protected var program = 0

    protected var vTexture = 0

    protected var textureId = 0

    protected var vMatrix = 0

    var matrix = FloatArray(16)

    init
    {
        program = ProgramUtil.linkProgram(verSource, fragSource)
    }

    open fun bindAttribute(textureId: Int)
    {
        Matrix.setIdentityM(matrix, 0)
        vPosition = glGetAttribLocation(program, "vPosition")
        glEnableVertexAttribArray(vPosition)
        vCoord = glGetAttribLocation(program, "vCoord")
        glEnableVertexAttribArray(vCoord)
        vTexture = glGetUniformLocation(program, "vTexture")
        this.textureId = textureId
        vMatrix = glGetUniformLocation(program, "vMatrix")
    }

    protected fun generateVerBuffer(floatArray: FloatArray): FloatBuffer
    {

        val verBuffer = ByteBuffer.allocateDirect(floatArray.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        verBuffer.put(floatArray)
        verBuffer.position(0)
        return verBuffer
    }

    open fun draw()
    {
        glUseProgram(program)
        onBindData()
        onBindTexture()
        onDraw()
    }


    open protected fun onBindData()
    {
        glUniformMatrix4fv(vMatrix, 1, false, matrix, 0)
    }


    open protected fun onBindTexture()
    {

    }

    open protected fun onDraw()
    {
//        glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
//        glDrawArrays(GL_TRIANGLES, 0, 6)
//        glBindTexture(GLES20.GL_TEXTURE_2D,0)
    }


}