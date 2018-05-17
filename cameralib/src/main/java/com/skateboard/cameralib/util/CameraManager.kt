package com.skateboard.cameralib.util

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.Size
import android.widget.Toast
import java.util.*

class CameraManager
{
    private var camera: Camera? = null

    private var width = 1080

    private var height = 1920

    constructor()


    constructor(width: Int, height: Int)
    {
        this.width = width
        this.height = height
    }

    fun setSize(width: Int, height: Int)
    {
        this.width = width
        this.height = height
    }

    fun open(cameraId: Int): Boolean
    {

        val tCamera = camera
        try
        {
            if (tCamera == null)
            {
                camera = Camera.open(cameraId)
                camera?.let {
                    val parameters = it.parameters
                    val preview = getBestSize(parameters.supportedPreviewSizes, width, height)
                    val picSize = getBestSize(parameters.supportedPictureSizes, width, height)
                    parameters.setPreviewSize(preview.width, preview.height)
                    parameters.setPictureSize(picSize.width, picSize.height)
                    it.parameters = parameters
                }
            }
        } catch (e: RuntimeException)
        {
            e.printStackTrace()
            return false
        }

        return true

    }


    private fun getBestSize(supportSizes: List<Size>, width: Int, height: Int): Size
    {

        Collections.sort(supportSizes, sizeComparator)
        val rate = width.toFloat() / height
        for (size in supportSizes)
        {
            if (equalRate(size, rate))
            {
                return size
            }
        }

        return supportSizes[supportSizes.size - 1]

    }


    private fun equalRate(s: Size, rate: Float): Boolean
    {
        val r = s.width.toFloat() / s.height.toFloat()
        return Math.abs(r - rate) <= 0.05
    }


    private val sizeComparator = Comparator<Size> { lhs, rhs ->
        when
        {
            lhs.height == rhs.height -> 0
            lhs.height > rhs.height -> 1
            else -> -1
        }
    }

    fun setPreviewTexture(surfaceTexture: SurfaceTexture)
    {
        camera?.setPreviewTexture(surfaceTexture) ?: println("not set camera fuck you")
    }


    fun startPreview()
    {
        camera?.startPreview() ?: println("not set camera start preview fuck you")
    }

    fun stopPreview()
    {
        camera?.stopPreview()
    }

    fun release()
    {
        camera?.stopPreview()
        camera?.release()
    }

}