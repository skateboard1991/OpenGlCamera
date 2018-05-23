package com.skateboard.cameralib.util

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.Size
import java.util.*
import android.view.Surface
import android.view.WindowManager


class CameraManager
{
    var camera: Camera? = null

    private var width = 1080

    private var height = 1920

    constructor()

    var previewSize:Size?=null

    var picSize:Size?=null

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
//                    previewSize = getBestSize(parameters.supportedPreviewSizes, width, height)
//                    picSize = getBestSize(parameters.supportedPictureSizes, width, height)
                    parameters.setPreviewSize(1280, 720)
                    parameters.setPictureSize(1280, 720)
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

    fun setBestDisplayOrientation(context:Context,cameraId: Int)
    {
        camera?.let {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(cameraId, info)
            val windowManager=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val rotation = windowManager.defaultDisplay.rotation

            var degrees = 0
            when (rotation)
            {
                Surface.ROTATION_0 -> degrees = 0
                Surface.ROTATION_90 -> degrees = 90
                Surface.ROTATION_180 -> degrees = 180
                Surface.ROTATION_270 -> degrees = 270
            }
            var result: Int
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
            {
                result = (info.orientation + degrees) % 360
                result = (360 - result) % 360  // compensate the mirror
            } else
            {  // back-facing
                result = (info.orientation - degrees + 360) % 360
            }
            it.setDisplayOrientation(result)
        }


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