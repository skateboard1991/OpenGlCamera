package com.skateboard.cameralib

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_record.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.concurrent.Executors

class RecordActivity : AppCompatActivity(), View.OnClickListener
{

    private var isRecording = false

    private val executeService = Executors.newSingleThreadExecutor()

    private var totalTime = 20000F

    private var minTime = 1000f

    private lateinit var outputFile: File

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            e.printStackTrace()
        }
        returnBtn.setOnClickListener(this)
        confirmBtn.setOnClickListener(this)
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

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun handleEvent(event: MessageEvent)
    {
        totalTime = event.totalTime

        minTime = event.minTime

        outputFile = event.outputFile

        cameraView.setOutputFile(event.outputFile)

        cameraView.setWaterMask(event.bitmap, event.waterX, event.waterY)

    }

    private val recordRunnable = Runnable {

        val recordTime = System.currentTimeMillis()
        var useTime = System.currentTimeMillis() - recordTime
        startRecord()
        isRecording = true
        while (isRecording)
        {
            useTime = System.currentTimeMillis() - recordTime
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
        if (useTime < minTime)
        {
            deleteFile()
            runOnUiThread {
                cameraView.startPreview()
                Toast.makeText(this@RecordActivity, getString(R.string.video_time_too_short), Toast.LENGTH_SHORT).show()
            }
        } else
        {
            showFinishBtns()
        }
        recordBtn.progress = 0F
    }

    private fun showFinishBtns()
    {
        runOnUiThread {
            recordBtn.visibility = View.GONE
            confirmBtn.visibility = View.VISIBLE
            returnBtn.visibility = View.VISIBLE
            confirmBtn.post {
                confirmBtn.animate().translationXBy(90 * resources.displayMetrics.density)
            }
            returnBtn.post {
                returnBtn.animate().translationXBy(-90 * resources.displayMetrics.density)
            }

        }
    }

    fun startRecord()
    {
        if (!outputFile.exists())
        {
            outputFile.createNewFile()
        }
        cameraView.startReceiveData()
    }

    private fun showRecordBtn()
    {

        cameraView.startPreview()
        runOnUiThread {
            recordBtn.visibility = View.VISIBLE
            confirmBtn.visibility = View.GONE
            returnBtn.visibility = View.GONE
            confirmBtn.translationX = 0f
            returnBtn.translationX = 0f

        }
    }

    fun stopRecord()
    {
        cameraView.stopReceiveData()
//        cameraView.stopPreview()
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

    override fun onClick(v: View?)
    {
        when (v?.id)
        {
            R.id.confirmBtn ->
            {
                finish()
            }

            R.id.returnBtn ->
            {
                deleteFile()
//                showRecordBtn()
                finish()
            }
        }
    }

    private fun deleteFile()
    {
        if (outputFile.exists())
        {
            outputFile.delete()
        }
    }

    companion object
    {
        fun startRecordActivity(context: Context, bitmap: Bitmap, minTime: Float, totalTime: Float, outputFile: File, waterX: Float = -1f, waterY: Float = 1F)
        {
            val event = MessageEvent(bitmap, minTime, totalTime, outputFile, waterX, waterY)
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

        data class MessageEvent(val bitmap: Bitmap, val minTime: Float = 1000F, val totalTime: Float = 2000F, val outputFile: File, val waterX: Float = -1F, val waterY: Float = 1F)
    }
}