package com.skateboard.cameralib

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.Camera
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.skateboard.cameralib.widget.CircleImageView
import kotlinx.android.synthetic.main.activity_record.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

class RecordActivity : AppCompatActivity(), View.OnClickListener
{

    private var isRecording = false

    private val executeService = Executors.newFixedThreadPool(1)

    private var totalTime = 20000F

    private var minTime = 1000f

    private var outputFile: File? = null

    private var waterBitmap: Bitmap? = null

    private var picBitmap: Bitmap? = null

    private var dirName = "cameraTest"

    private val reenterLock = ReentrantLock()

    private val condition = reenterLock.newCondition()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        returnBtn.setOnClickListener(this)
        confirmBtn.setOnClickListener(this)
        backArrow.setOnClickListener(this)
        recordBtn.setOnClickListener(this)
        recordBtn.listener = object : CircleImageView.OnProgressTouchListener
        {
            override fun onLongClickUp()
            {
                isRecording = false
            }

            override fun onClick()
            {

                cameraView.takePicture(null, null, pictureCallback)
            }

            override fun onLongpress()
            {
                executeService.execute(recordRunnable)
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun handleEvent(event: MessageEvent)
    {
        totalTime = event.totalTime

        minTime = event.minTime

        dirName = event.dirName

        waterBitmap = event.bitmap

        cameraView.setWaterMask(event.bitmap, event.waterX, event.waterY)

    }

    override fun onStart()
    {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    private val recordRunnable = Runnable {

        val recordTime = System.currentTimeMillis()
        var useTime = System.currentTimeMillis() - recordTime
        outputFile = generaeteOutputFile(false)
        val tempFile = outputFile
        if (tempFile != null)
        {
            cameraView.setOutputFile(tempFile)
        }
        startRecord()
        isRecording = true
        while (isRecording)
        {
            try
            {
                reenterLock.lock()
                useTime = System.currentTimeMillis() - recordTime
                if (useTime <= totalTime)
                {
                    val progress = useTime / totalTime * 100
                    runOnUiThread {
                        recordBtn.progress = progress
                        try
                        {
                            reenterLock.lock()
                            condition.signalAll()
                        } catch (e: Exception)
                        {
                            e.printStackTrace()
                        } finally
                        {
                            reenterLock.unlock()
                        }

                    }
                    condition.await()

                } else
                {
                    isRecording = false
                }
            } catch (e: Exception)
            {
                e.printStackTrace()
            } finally
            {
                reenterLock.unlock()
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


    private val pictureCallback = Camera.PictureCallback { data, camera ->

        if (data != null)
        {
            picBitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            val bitmap = picBitmap
            if (bitmap != null)
            {
                val tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(tempBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint)
                canvas.drawBitmap(waterBitmap, 0.0f, 0.0f, paint)
                picBitmap = tempBitmap
            }
            showFinishBtns()
        }

    }


    private fun showFinishBtns()
    {
        runOnUiThread {
            recordBtn.visibility = View.GONE
            backArrow.visibility = View.GONE
            confirmBtn.visibility = View.VISIBLE
            returnBtn.visibility = View.VISIBLE
            confirmBtn.post {
                confirmBtn.animate().setDuration(500L).translationXBy(75 * resources.displayMetrics.density)
            }
            returnBtn.post {
                returnBtn.animate().setDuration(500L).translationXBy(-75 * resources.displayMetrics.density)
            }

        }
    }

    fun startRecord()
    {
        if (outputFile?.exists() == false)
        {
            outputFile?.createNewFile()
        }
        cameraView.startReceiveData()
    }

    private fun showRecordBtn()
    {

        cameraView.startPreview()
        runOnUiThread {
            backArrow.visibility = View.VISIBLE
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
        finish()
    }


    override fun onStop()
    {
        super.onStop()
        EventBus.getDefault().unregister(this)
        if (waterBitmap?.isRecycled == false)
        {
            waterBitmap?.recycle()
        }
        if (picBitmap?.isRecycled == false)
        {
            picBitmap?.recycle()
        }
    }


    override fun onClick(v: View?)
    {
        when (v?.id)
        {
            R.id.confirmBtn ->
            {
                picBitmap?.let {
                    savePicture(it)

                }
                val intent = Intent()
                intent.putExtra(KEY_OUTPUT_FILEPATH, outputFile?.absolutePath ?: "")
                setResult(Activity.RESULT_OK, intent)
                finish()
            }

            R.id.returnBtn ->
            {
                deleteFile()
                setResult(Activity.RESULT_CANCELED)
//                showRecordBtn()
                finish()
            }

            R.id.backArrow ->
            {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun savePicture(bitmap: Bitmap)
    {
        outputFile = generaeteOutputFile(true)
        var writer: FileOutputStream? = null
        try
        {
            writer = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, writer)
            writer.flush()
        } catch (e: Exception)
        {
            e.printStackTrace()
        } finally
        {
            writer?.close()
        }
    }

    private fun deleteFile()
    {
        if (outputFile?.exists() == true)
        {
            outputFile?.delete()
        }
    }


    private fun generaeteOutputFile(isPicture: Boolean): File
    {
        val dir = File(Environment.getExternalStorageDirectory(), dirName)
        if (!dir.exists())
        {
            dir.mkdirs()
        }

        val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())
        return File(dir.absolutePath, if (isPicture) "$fileName.jpg" else "$fileName.mp4")

    }

    companion object
    {
        val KEY_OUTPUT_FILEPATH = "key_output_filepath"

        fun startRecordActivity(activity: Activity, requestCode: Int, bitmap: Bitmap, minTime: Float, totalTime: Float, dirName: String, waterX: Float = -1f, waterY: Float = 1F)
        {
            val event = MessageEvent(bitmap, minTime, totalTime, dirName, waterX, waterY)
            EventBus.getDefault().postSticky(event)
            val intent = Intent(activity, RecordActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }

        fun startRecordActivity(fragment: Fragment, requestCode: Int, bitmap: Bitmap, minTime: Float, totalTime: Float, dirName: String, waterX: Float = -1f, waterY: Float = 1F)
        {
            val event = MessageEvent(bitmap, minTime, totalTime, dirName, waterX, waterY)
            EventBus.getDefault().postSticky(event)
            val intent = Intent(fragment.activity, RecordActivity::class.java)
            fragment.startActivityForResult(intent, requestCode)
        }

        fun startRecordActivity(context: Context, event: MessageEvent)
        {
            val intent = Intent(context, RecordActivity::class.java)

            EventBus.getDefault().postSticky(event)

            context.startActivity(intent)
        }

        data class MessageEvent(val bitmap: Bitmap, val minTime: Float = 1000F, val totalTime: Float = 2000F, val dirName: String, val waterX: Float = -1F, val waterY: Float = 1F)
    }
}