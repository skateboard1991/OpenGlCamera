package com.skateboard.cameralib.util

import android.content.Context
import android.content.res.Resources
import java.io.BufferedReader
import java.io.InputStreamReader

object SourceReaderUtil
{
    fun readText(context: Context,resId:Int):String
    {
        val textBuilder=StringBuilder()
        try
        {
            val bufferedReader=BufferedReader(InputStreamReader(context.resources.openRawResource(resId)))
            var line=bufferedReader.readLine()
            while(line!=null)
            {
                textBuilder.append(line)
                line=bufferedReader.readLine()
            }
        }
        catch (e:Resources.NotFoundException)
        {
            e.printStackTrace()
        }
        catch (e:Exception)
        {
            e.printStackTrace()
        }
        return textBuilder.toString()

    }

}