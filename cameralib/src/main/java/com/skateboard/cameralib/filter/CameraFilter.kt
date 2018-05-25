package com.skateboard.cameralib.filter

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.Matrix
import java.nio.FloatBuffer

open class CameraFilter(verSource: String, fragSource: String) : BaseFilter(verSource, fragSource)
{

    private var verData: FloatArray = floatArrayOf(

            -1f, 1f,
            -1f, -1f,
            1f, 1f,
            1f, -1f

    )

    private var textureCoor = floatArrayOf(

            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f
    )


    private lateinit var verPositionBuffer: FloatBuffer

    private lateinit var vCoordBuffer: FloatBuffer

    fun handleMatrix(matrix: FloatArray?)
    {
        Matrix.setIdentityM(matrix, 0)
        Matrix.rotateM(matrix, 0, 180f, 1f, 0f, 0f)
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, matrix, 0)
    }

    override fun bindAttribute(textureId: Int)
    {
        super.bindAttribute(textureId)
        verPositionBuffer = generateVerBuffer(verData)
        vCoordBuffer = generateVerBuffer(textureCoor)
    }

    override fun onBindData()
    {
        super.onBindData()
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, verPositionBuffer)
        glEnableVertexAttribArray(vPosition)
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, vCoordBuffer)
        glEnableVertexAttribArray(vCoord)
    }

    override fun onBindTexture()
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(vTexture, 0)
    }


    override fun onDraw()
    {
        handleMatrix(matrix)
        glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        glBindTexture(GLES20.GL_TEXTURE_2D,0)
    }


}