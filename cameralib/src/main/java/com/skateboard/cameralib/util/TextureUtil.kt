package com.skateboard.cameralib.util


import android.opengl.GLES11Ext
import android.opengl.GLES20
import javax.microedition.khronos.opengles.GL10

object TextureUtil
{
    val TAG="TextureUtil"

    fun createTextureObj(target:Int):Int
    {
        val textureObj = intArrayOf(0)
        GLES20.glGenTextures(1, textureObj, 0)
        if (textureObj[0] == 0)
        {
            LogUtil.logW(TAG,"cretae texture failed")
            return 0
        }
        GLES20.glBindTexture(target, textureObj[0])
        GLES20.glTexParameteri(target,
                GL10.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(target,
                GL10.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target,
                GL10.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(target,
                GL10.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(target, 0)
        return textureObj[0]
    }

}