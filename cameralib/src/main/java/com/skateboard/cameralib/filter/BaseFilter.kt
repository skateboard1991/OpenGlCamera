package com.skateboard.cameralib.filter

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES20.*
import com.skateboard.cameralib.util.ProgramUtil
import com.skateboard.cameralib.util.TextureUtil
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class BaseFilter
{
    protected var vPosition = 0

    protected var vCoord = 0

    protected var vMatrix = 0

    protected var program = 0

    protected var vTexture = 0

    protected var textureId = 0

    open var verData = floatArrayOf(

            -1f, 1f, 1f, 0f,
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            1f, 1f, 1f, 0f
    )

    val FLOAT_SIZE = 4

    val POSITION_SIZE = 2


    fun bindAttribute(verSource: String, fragSource: String, textureId: Int)
    {

        val verBuffer = generateVerBuffer()

        program = ProgramUtil.linkProgram(verSource, fragSource)
        glUseProgram(program)
        vPosition = glGetAttribLocation(program, "vPosition")
        glVertexAttribPointer(vPosition, POSITION_SIZE, GL_FLOAT, false, FLOAT_SIZE * 4, verBuffer)
        glEnableVertexAttribArray(vPosition)
        vCoord = glGetAttribLocation(program, "vCoord")
        glVertexAttribPointer(vCoord, POSITION_SIZE, GL_FLOAT, false, FLOAT_SIZE * 4, verBuffer)
        glEnableVertexAttribArray(vCoord)
        vMatrix = glGetAttribLocation(program, "vMatrix")
        glEnableVertexAttribArray(vMatrix)

        vTexture = glGetUniformLocation(program, "vTexture")

        this.textureId = TextureUtil.createTextureObj()
        glActiveTexture(GLES20.GL_TEXTURE0)
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        glUniform1i(vTexture, 0)

    }

    private fun generateVerBuffer(): Buffer
    {

        val verBuffer = ByteBuffer.allocateDirect(verData.size * FLOAT_SIZE)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        verBuffer.put(verData)
        verBuffer.position(0)
        return verBuffer
    }

    fun draw()
    {
        glClear(GLES20.GL_COLOR_BUFFER_BIT)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
    }


}