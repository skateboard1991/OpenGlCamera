package com.skateboard.cameralib.util

import android.util.Log

object LogUtil
{
    var ENABLE=true

    fun logW(tag:String,message:String)
    {
        if(ENABLE)
        {
            Log.w(tag, message)
        }
    }

    fun logE(tag: String,message: String)
    {
        if(ENABLE)
        {
            Log.e(tag,message)
        }
    }

}