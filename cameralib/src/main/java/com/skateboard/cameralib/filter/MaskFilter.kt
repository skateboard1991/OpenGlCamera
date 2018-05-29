package com.skateboard.cameralib.filter

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLUtils
import java.nio.FloatBuffer

class MaskFilter(verSource: String, fragSource: String) : BaseFilter(verSource, fragSource)
{


    private var verData: FloatArray = floatArrayOf(

            -1f, 1f,
            -1f, 0f,
            0f, 1f,
            0f, 0f

    )

    private var textureCoor = floatArrayOf(

            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
    )


    private lateinit var verPositionBuffer: FloatBuffer

    private lateinit var vCoordBuffer: FloatBuffer

    override fun bindAttribute(textureId: Int)
    {
        super.bindAttribute(textureId)
        verPositionBuffer = generateVerBuffer(verData)
        vCoordBuffer = generateVerBuffer(textureCoor)
    }

    fun setMaskImg(bitmap: Bitmap, x: Int, y: Int)
    {
        glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
        glBindTexture(GL_TEXTURE_2D, 0)
        bitmap.recycle()
    }


    override fun draw()
    {
        glEnable(GLES20.GL_BLEND)
        glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA)
        super.draw()
        glDisable(GLES20.GL_BLEND)
    }

    override fun onBindData()
    {
        super.onBindData()
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, verPositionBuffer)
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, vCoordBuffer)
    }

    override fun onBindTexture()
    {
        GLES20.glActiveTexture(GL_TEXTURE1)
        GLES20.glBindTexture(GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(vTexture, 1)
    }


    override fun onDraw()
    {
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}