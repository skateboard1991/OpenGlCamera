package com.skateboard.cameralib.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

object GLDateUtil
{

    fun generateFloatBuffer(array: FloatArray): FloatBuffer
    {
        val floatBuffer = ByteBuffer.allocateDirect(array.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        floatBuffer.put(array)
        floatBuffer.position(0)
        return floatBuffer
    }

    fun generateIntBuffer(array: IntArray): IntBuffer
    {
        val intBuffer = ByteBuffer.allocateDirect(array.size * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer()
        intBuffer.put(array)
        intBuffer.position(0)
        return intBuffer
    }

}