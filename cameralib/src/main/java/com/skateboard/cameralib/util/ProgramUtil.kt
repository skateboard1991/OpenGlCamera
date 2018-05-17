package com.skateboard.cameralib.util

import android.opengl.GLES20
import android.opengl.GLES20.*

object ProgramUtil
{


    fun linkProgram(verSource: String, fragSource: String): Int
    {
        val verShader = ShaderUtil.compileShader(GL_VERTEX_SHADER, verSource)
        val fragShader = ShaderUtil.compileShader(GL_FRAGMENT_SHADER, fragSource)
        val program = linkProgram(verShader, fragShader)
        return program
    }


    fun linkProgram(verShader: Int, fragShader: Int): Int
    {
        val program = GLES20.glCreateProgram()
        if (program == 0)
        {
            LogUtil.logW(ShaderUtil.TAG, "create program failed")
            return 0
        }
        GLES20.glAttachShader(program, verShader)
        GLES20.glAttachShader(program, fragShader)
        GLES20.glLinkProgram(program)
        val status = intArrayOf(0)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0)
        {
            LogUtil.logW(ShaderUtil.TAG, "link program failed")
            GLES20.glDeleteProgram(program)
        }
        return program
    }

}