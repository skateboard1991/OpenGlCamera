package com.skateboard.cameralib.filter

import android.opengl.GLES20

class ShowFilter:BaseFilter()
{
    override var verData = floatArrayOf(

            -1f,1f,0f,1f,
            -1f,-1f,0f,0f,
            1f,1f,1f,1f,
            1f,-1f,1f,0f

    )

    override fun bindAttribute(verSource: String, fragSource: String, textureId: Int)
    {
        super.bindAttribute(verSource, fragSource, textureId)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId)
    }

    fun setImage2D(width:Int,height:Int,format:Int)
    {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,format,width,height,0,format,GLES20.GL_UNSIGNED_BYTE,null)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0)
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

    }
}