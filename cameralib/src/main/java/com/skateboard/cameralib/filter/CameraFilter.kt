package com.skateboard.cameralib.filter

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix

open class CameraFilter : BaseFilter()
{

    override var verData: FloatArray= floatArrayOf(

//            -1f,1f,0f,0f
//            -1f,-1f,1f,0f,
//            1f,1f,0f,1f,
//            1f,-1f,1f,1f


            -1f,1f,1.0f, 1.0f,
            -1f,-1f,0.0f, 1.0f,
            1f,1f,1.0f, 0.0f,
            1f,-1f,0.0f, 0.0f

    )

    fun handleMatrix(matrix: FloatArray?)
    {
        Matrix.setIdentityM(matrix,0)
        Matrix.rotateM(matrix,0,180f,1f,0f,0f)
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, matrix, 0)
    }


    override fun onDraw()
    {
        handleMatrix(matrix)
//        super.onDraw()
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
    }

    override fun onBindTexture()
    {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(vTexture, 0)
    }
}