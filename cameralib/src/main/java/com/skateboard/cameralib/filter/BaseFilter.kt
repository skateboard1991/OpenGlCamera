package com.skateboard.cameralib.filter

import android.opengl.GLES20
import com.skateboard.cameralib.util.ProgramUtil

open class BaseFilter
{
    protected var vPosition = 0

    protected var vCoord = 0

    protected var vMatrix = 0

    protected var program = 0

    open var verData=FloatArray(16)


    fun bindAttribute(verSource: String, fragSource: String)
    {
        program = ProgramUtil.linkProgram(verSource, fragSource)
        GLES20.glUseProgram(program)
        vPosition = GLES20.glGetAttribLocation(program, "vPosition")
        GLES20.glEnableVertexAttribArray(vPosition)
        vCoord = GLES20.glGetAttribLocation(program, "vCoord")
        GLES20.glEnableVertexAttribArray(vCoord)
        vMatrix = GLES20.glGetAttribLocation(program, "vMatrix")
        GLES20.glEnableVertexAttribArray(vMatrix)
    }

    fun draw()
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    }


}