package com.skateboard.cameralib.util

import android.opengl.GLES20
import android.opengl.GLES20.*

object ShaderUtil
{
    val TAG="ShaderUtil"

    val FRAG_SHADER="precision mediump float;\n" +
            "uniform sampler2D vTexture;\n" +
            "varying vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(vTexture, textureCoordinate);\n" +
            "}"
    val VER_SHADER="attribute vec4 vPosition;\n" +
            "attribute vec2 vCoord;\n" +
            "uniform mat4 vMatrix;\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main(){\n" +
            "    gl_Position =vMatrix*vPosition;\n" +
            "    textureCoordinate = vCoord;\n" +
            "}"

    val C_FRAG_SHADER="#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES vTexture;\n" +
            "varying vec2 textureCoordinate;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(vTexture, textureCoordinate);\n" +
            "}"

    val C_VER_SHADER="attribute vec4 vPosition;\n" +
            " attribute vec2 vCoord;\n" +
            " uniform mat4 vMatrix;\n" +
            " varying vec2 textureCoordinate;\n" +
            "\n" +
            " void main(){\n" +
            "     gl_Position =vMatrix*vPosition;\n" +
            "     textureCoordinate = vCoord;\n" +
            " }"


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
            return 0
        }
        return shader
    }



}