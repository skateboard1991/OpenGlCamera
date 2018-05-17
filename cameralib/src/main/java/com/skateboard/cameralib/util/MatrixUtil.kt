package com.skateboard.cameralib.util

import android.opengl.Matrix

object MatrixUtil
{
    fun rotate(m: FloatArray, angle: Float): FloatArray
    {
        Matrix.rotateM(m, 0, angle, 0f, 0f, 1f)
        return m
    }

    fun flip(m: FloatArray, x: Boolean, y: Boolean): FloatArray
    {
        if (x || y)
        {
            Matrix.scaleM(m, 0, (if (x) -1 else 1).toFloat(), (if (y) -1 else 1).toFloat(), 1f)
        }
        return m
    }

    fun getShowMatrix(matrix: FloatArray, imgWidth: Int, imgHeight: Int, viewWidth: Int, viewHeight: Int)
    {
        if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0)
        {
            val sWhView = viewWidth.toFloat() / viewHeight
            val sWhImg = imgWidth.toFloat() / imgHeight
            val projection = FloatArray(16)
            val camera = FloatArray(16)
            if (sWhImg > sWhView)
            {
                Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1f, 1f, 1f, 3f)
            } else
            {
                Matrix.orthoM(projection, 0, -1f, 1f, -sWhImg / sWhView, sWhImg / sWhView, 1f, 3f)
            }
            Matrix.setLookAtM(camera, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0)
        }
    }

}