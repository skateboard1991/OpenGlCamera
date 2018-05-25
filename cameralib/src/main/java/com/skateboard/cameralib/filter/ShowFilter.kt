package com.skateboard.cameralib.filter

import android.opengl.GLES20
import java.nio.FloatBuffer

class ShowFilter(verSource: String, fragSource: String) : BaseFilter(verSource, fragSource)
{

    private var verData: FloatArray = floatArrayOf(

            -1f, 1f,
            -1f, -1f,
            1f, 1f,
            1f, -1f

    )

    private var textureCoor = floatArrayOf(

            0f, 1f,
            0f, 0f,
            1f, 1f,
            1f, 0f
    )

    private lateinit var verPositionBuffer: FloatBuffer

    private lateinit var vCoordBuffer: FloatBuffer


    override fun bindAttribute(textureId: Int)
    {
        super.bindAttribute(textureId)
        verPositionBuffer = generateVerBuffer(verData)
        vCoordBuffer = generateVerBuffer(textureCoor)
    }

    fun setImage2D(width: Int, height: Int, format: Int)
    {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    override fun onBindData()
    {
        super.onBindData()
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, verPositionBuffer)
        GLES20.glEnableVertexAttribArray(vCoord)
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, vCoordBuffer)
    }

    override fun onBindTexture()
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(vTexture, 1)
    }

    override fun onDraw()
    {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
    }
}