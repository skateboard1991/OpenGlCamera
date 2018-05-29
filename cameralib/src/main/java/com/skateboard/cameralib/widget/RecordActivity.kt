package com.skateboard.cameralib.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import com.skateboard.cameralib.R
import kotlinx.android.synthetic.main.activity_record.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.concurrent.Executors

class RecordActivity : AppCompatActivity()
{

    private var isRecording = false

    private val executeService = Executors.newSingleThreadExecutor()

    private var totalTime = 20000F

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        recordBtn.setOnTouchListener { v, event ->

            when (event?.action)
            {

                MotionEvent.ACTION_DOWN ->
                {
                    executeService.execute(recordRunnable)
                }

                MotionEvent.ACTION_UP ->
                {
                    isRecording = false
                }


            }

            true
        }
    }

    override fun onStart()
    {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    fun handleEvent(event: MessageEvent)
    {
        totalTime = event.totalTime

        cameraView.setOutputFile(event.outputFile)

        cameraView.setWaterMask(event.bitmap)

    }

    private val recordRunnable = Runnable {

        val recordTime = System.currentTimeMillis()
        startRecord()
        isRecording = true
        while (isRecording)
        {
            val useTime = System.currentTimeMillis() - recordTime
            if (useTime <= totalTime)
            {
                val progress = useTime / totalTime * 100
                recordBtn.progress = progress
            } else
            {
                isRecording = false
            }
        }
        stopRecord()
        recordBtn.progress = 0F
    }


    fun startRecord()
    {
        cameraView.startReceiveData()
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

    override fun onDestroy()
    {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    companion object
    {
        fun startRecordActivity(context: Context, bitmap: Bitmap, totalTime: Float, outputFile: File)
        {
            val event = MessageEvent(bitmap, totalTime, outputFile)
            EventBus.getDefault().postSticky(event)
            val intent = Intent(context, RecordActivity::class.java)
            context.startActivity(intent)
        }

        fun startRecordActivity(context: Context, event: MessageEvent)
        {
            val intent = Intent(context, RecordActivity::class.java)

            EventBus.getDefault().postSticky(event)

            context.startActivity(intent)
        }

        data class MessageEvent(val bitmap: Bitmap, val totalTime: Float = 2000F, val outputFile: File)
    }
}