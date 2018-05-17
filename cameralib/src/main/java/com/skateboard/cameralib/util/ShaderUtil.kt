package com.skateboard.cameralib.util

import android.opengl.GLES20
import android.opengl.GLES20.*

object ShaderUtil
{
    val TAG="ShaderUtil"

    fun compileShader(type:Int,source:String):Int
    {

        val shader=glCreateShader(type)
        if(shader==0)
        {
            LogUtil.logW(TAG,"create shader failed")
            return 0
        }
        glShaderSource(shader,source)
        glCompileShader(shader)
        val status= intArrayOf(0)
        glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,status,0)
        if(status[0]==0)
        {
            LogUtil.logW(TAG,"compile shader failed")
            glDeleteShader(shader)
        }
        return shader
    }



}