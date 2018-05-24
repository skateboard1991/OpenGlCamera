package com.skateboard.cameratest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import com.skateboard.cameralib.codec.CameraRecorder
import com.skateboard.cameralib.codec.CameraRecorderTest
import com.skateboard.cameralib.widget.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(), CameraView.OnFrameCallback
{


    private val cameraRecorder = CameraRecorder()

    private val cameraRecorderTest=CameraRecorderTest()

    private val handler=Handler(Looper.getMainLooper())

    private var isRecording=false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraView.frameCallback=this
        recordBtn.setOnClickListener {


            if(isRecording)
            {
                stopRecord()
                isRecording=false
            }
            else
            {
                startRecord()
                isRecording=true
            }


        }
    }

    fun startRecord()
    {
        cameraView.startReceiveData()
        cameraRecorderTest.start()
        handler.postDelayed({
            stopRecord()},30000)
    }

    fun stopRecord()
    {
        cameraView.stopReceiveData()
        cameraRecorderTest.cancel()
    }

    override fun onFrameBack(data: ByteArray)
    {
        cameraRecorderTest.feedData(data)
    }

    override fun onPreviewSizeChanged(width: Int, height: Int)
    {
        cameraRecorderTest.setSavePath(Environment.getExternalStorageDirectory().absolutePath+File.separator+"cameraTest/video","mp4")
        cameraRecorderTest.prepare(720, 1280)

    }


    fun generateFilePath():String
    {
        val dir=File(Environment.getExternalStorageDirectory(),"cameraTest")
        if(!dir.exists())
        {
            dir.mkdirs()
        }

        val file=File(dir.absolutePath,"test.mp4")
        return file.absolutePath
    }

    override fun onResume()
    {
        super.onResume()
        cameraView.onResume()
    }

    override fun onPause()
    {
        super.onPause()
        cameraView.onPause()
    }

}
