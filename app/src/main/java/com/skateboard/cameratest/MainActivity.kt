package com.skateboard.cameratest

import android.graphics.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import com.skateboard.cameralib.widget.RecordActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity()
{


    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recordBtn.setOnClickListener{

            RecordActivity.startRecordActivity(this,convertMessageToBitmap("我不说你可能不知道，这是个水印"),20000f,generateFilePath())

        }
    }

    private fun convertMessageToBitmap(message:String):Bitmap
    {
        val bitmap=Bitmap.createBitmap(1080,190,Bitmap.Config.ARGB_8888)
        val canvasTemp = Canvas(bitmap)
        canvasTemp.drawColor(Color.TRANSPARENT)
        val  p = Paint()
        p.color=Color.RED
        p.textSize=60f
        canvasTemp.drawText(message,0f,180f,p)
        return bitmap
    }

    fun generateFilePath(): File
    {
        val dir = File(Environment.getExternalStorageDirectory(), "cameraTest")
        if (!dir.exists())
        {
            dir.mkdirs()
        }

        return File(dir.absolutePath, "test.mp4")
    }
}