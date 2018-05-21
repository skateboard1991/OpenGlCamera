package com.skateboard.cameralib.filter

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix

open class CameraFilter:BaseFilter()
{
    private var matrix=FloatArray(16)

    private var vMatrix=0

    override fun bindAttribute(verSource: String, fragSource: String, textureId: Int)
    {
        super.bindAttribute(verSource, fragSource, textureId)
        vMatrix=GLES20.glGetUniformLocation(program,"vMatrix")

    }

    fun handleMatrix(matrix: FloatArray?)
    {
        Matrix.setIdentityM(matrix,0)
        Matrix.rotateM(matrix,0,90f,0f,0f,1f)
        Matrix.rotateM(matrix,0,180f,0f,1f,0f)
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, matrix, 0)
    }


    override fun onDraw()
    {
        handleMatrix(matrix)
        super.onDraw()
    }

    override fun onBindTexture()
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(vTexture, 0)
    }
}