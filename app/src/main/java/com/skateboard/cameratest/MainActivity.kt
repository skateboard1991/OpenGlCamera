package com.skateboard.cameratest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.skateboard.cameralib.widget.CameraRender
import com.skateboard.cameralib.widget.CameraView

class MainActivity : AppCompatActivity() {

    private lateinit var cameraView: CameraView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraView=CameraView(this)
        setContentView(cameraView)
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
