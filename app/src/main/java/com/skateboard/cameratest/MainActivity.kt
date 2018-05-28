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

class MainActivity : AppCompatActivity()
{


    private val cameraRecorder = CameraRecorder()

    private val cameraRecorderTest = CameraRecorderTest()

    private val handler = Handler(Looper.getMainLooper())

    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recordBtn.setOnClickListener {


            if (isRecording)
            {
                stopRecord()
                isRecording = false
            } else
            {
                startRecord()
                isRecording = true
            }


        }
    }

    fun startRecord()
    {
        cameraView.startReceiveData()
        handler.postDelayed({
            stopRecord()
        }, 10000)
    }

    fun stopRecord()
    {
        cameraView.stopReceiveData()
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
