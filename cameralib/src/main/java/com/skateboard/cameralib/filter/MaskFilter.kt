package com.skateboard.cameralib.filter

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLUtils

class MaskFilter : BaseFilter()
{

    override var verData = floatArrayOf(

             -1f,1f,0f,1f,
            -1f,0f,0f,0f,
            0f,1f,1f,1f,
            0f,0f,1f,0f

    )

    fun setMaskImg(bitmap: Bitmap, x: Int, y: Int)
    {
//        glViewport(x, y, 90, 90)
        glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
        glBindTexture(GL_TEXTURE_2D, 0)
        bitmap.recycle()
    }

    override fun onBindTexture()
    {
        GLES20.glActiveTexture(GL_TEXTURE1)
        GLES20.glBindTexture(GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(vTexture, 1)
    }

    override fun onDraw()
    {
        glDrawArrays(GL_TRIANGLE_STRIP,0,4)

    }
}