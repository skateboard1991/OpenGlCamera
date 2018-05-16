package com.skateboard.cameralib.util

import android.graphics.SurfaceTexture
import android.hardware.Camera
import java.util.*

class CameraManager
{
    private var camera: Camera? = null

    init
    {

    }

    fun open(cameraId: Int,previewWidth:Int,previewHeight:Int)
    {
        val tCamera=camera
        if(tCamera==null)
        {
            camera = Camera.open(cameraId)
            camera?.let {
                val parameters = it.parameters
                val preiviewSize = getBestPreviewSize(parameters.supportedPreviewSizes, previewWidth, previewHeight)
                val picSize = getBestPicSize(parameters.supportedPictureSizes, previewWidth, previewHeight)
                parameters.setPreviewSize(preiviewSize.width, preiviewSize.height)
                parameters.setPictureSize(picSize.width, picSize.height)
                it.parameters = parameters
            }
        }

    }

    private fun getBestPreviewSize(list: List<Camera.Size>, screenWidth: Int, screenHeight: Int): Camera.Size
    {
        Collections.sort<Camera.Size>(list, sizeComparator)

        var i = 0
        for (s in list)
        {
            if (s.height >= screenWidth && isRateEqual(s, screenWidth.toFloat() / screenHeight))
            {
                break
            }
            i++
        }
        if (i == list.size)
        {
            i = 0
        }
        return list[i]
    }


    private fun isRateEqual(size: Camera.Size, rate: Float): Boolean
    {
        val r = size.width.toFloat() / size.height.toFloat()
        return Math.abs(r - rate) <= 0.03
    }


    private fun getBestPicSize(list: List<Camera.Size>, screenWidth: Int, screenHeight: Int): Camera.Size
    {
        return getBestPreviewSize(list, screenWidth, screenHeight)
    }


    private val sizeComparator = Comparator<Camera.Size> { lhs, rhs ->
        when
        {
            lhs.height == rhs.height -> 0
            lhs.height > rhs.height -> 1
            else -> -1
        }
    }

    fun setPreviewTexture(surfaceTexture:SurfaceTexture)
    {
        camera?.setPreviewTexture(surfaceTexture)
    }


    fun startPreview()
    {
        camera?.startPreview()
    }

}