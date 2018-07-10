package com.skateboard.cameralib.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.skateboard.cameralib.R
import android.support.v4.view.MotionEventCompat
import android.text.method.Touch.onTouchEvent


class CircleImageView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet)
{
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var normalColor = Color.WHITE

    private var outCircleNormalColor = Color.GRAY

    private var activeColor = Color.YELLOW

    private var outerCircleWidth = 10f

    private var spacing = 10f

    private lateinit var gestureDetector: GestureDetector

    var listener: OnProgressTouchListener? = null

    private var isLongClick = false

    var progress = 0f
        set(value)
        {
            field = value
            if (isAttachedToWindow)
            {
                isActive = field != 0f
                post {
                    invalidate()
                }

            }
        }

    private var isActive = false

    private val gestureDetectorListener = object : GestureDetector.SimpleOnGestureListener()
    {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean
        {
            isLongClick = false
            listener?.onClick()
            return super.onSingleTapConfirmed(e)
        }

        override fun onLongPress(e: MotionEvent?)
        {
            isLongClick = true
            postInvalidate()
            listener?.onLongpress()
        }
    }


    constructor(context: Context) : this(context, null)

    init
    {
        if (attributeSet != null)
        {
            parseAttrs(attributeSet)
        }

        paint.strokeWidth = outerCircleWidth
        initGestureDetector()
    }

    private fun initGestureDetector()
    {
        gestureDetector = GestureDetector(context, gestureDetectorListener)
        gestureDetector.setIsLongpressEnabled(true)
    }


    interface OnProgressTouchListener
    {
        fun onClick()

        fun onLongpress()

        fun onLongClickUp()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean
    {
        gestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event?.action)
        {
            MotionEvent.ACTION_DOWN -> isLongClick = false
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (isLongClick)
            {
                isLongClick = false
                postInvalidate()
                listener?.onLongClickUp()

            }
        }
        return true
    }

    private fun parseAttrs(attributeSet: AttributeSet)
    {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.CircleImageView)
        outCircleNormalColor=typedArray.getColor(R.styleable.CircleImageView_outCircleNormalColor,outCircleNormalColor)
        outerCircleWidth = typedArray.getDimension(R.styleable.CircleImageView_outerCircleWidth, 10f)
        normalColor = typedArray.getColor(R.styleable.CircleImageView_normalColor, Color.WHITE)
        activeColor = typedArray.getColor(R.styleable.CircleImageView_activeColor, Color.YELLOW)
        spacing = typedArray.getDimension(R.styleable.CircleImageView_spacing, 10f)
        typedArray.recycle()
    }

    override fun onDraw(canvas: Canvas?)
    {
        super.onDraw(canvas)
        canvas?.let {

            val centerX = paddingLeft + (width - paddingLeft - paddingRight).toFloat() / 2
            val centerY = paddingTop + (height - paddingTop - paddingBottom).toFloat() / 2
            drawOuterCircle(it, centerX, centerY)
            drawInnerCircle(it, centerX, centerY)

        }
    }


    private fun drawOuterCircle(canvas: Canvas, centerX: Float, centerY: Float)
    {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outerCircleWidth
        paint.color = outCircleNormalColor
        val radius = centerY - paddingTop - outerCircleWidth / 2
        canvas.drawCircle(centerX, centerY, radius, paint)

        if (isActive)
        {
            paint.color = activeColor
            val percent = progress / 100f
            canvas.drawArc(RectF(paddingLeft.toFloat() + outerCircleWidth / 2, paddingTop.toFloat() + outerCircleWidth / 2, width.toFloat() - paddingRight - outerCircleWidth / 2, height
                    .toFloat() - paddingBottom - outerCircleWidth / 2), -90f, percent * 360, false, paint)
        }
    }

    private fun drawInnerCircle(canvas: Canvas, centerX: Float, centerY: Float)
    {
        paint.style = Paint.Style.FILL
        if (isActive)
        {
            paint.color = activeColor
        } else
        {
            paint.color = normalColor
        }
        val radius = (centerY - paddingTop - outerCircleWidth / 2) - spacing
        canvas.drawCircle(centerX, centerY, radius, paint)
    }
}