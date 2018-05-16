package com.skateboard.cameralib.util

import android.opengl.GLES20

object ProgramUtil
{
    fun linkProgram(verShader:Int,fragShader:Int):Int
    {
        val program= GLES20.glCreateProgram()
        if(program==0)
        {
            LogUtil.logW(ShaderUtil.TAG,"create program failed")
            return 0
        }
        GLES20.glAttachShader(program, verShader)
        GLES20.glAttachShader(program, fragShader)
        GLES20.glLinkProgram(program)
        val status= intArrayOf(0)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if(status[0]==0)
        {
            LogUtil.logW(ShaderUtil.TAG,"link program failed")
            GLES20.glDeleteProgram(program)
        }
        return program
    }

    fun useProgram(program:Int)
    {
        GLES20.glUseProgram(program)
    }
}