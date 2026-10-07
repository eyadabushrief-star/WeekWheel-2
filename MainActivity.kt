package com.example.weekwheel

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    private lateinit var wheel: WeekWheelView
    private lateinit var current: TextView
    private val days = arrayOf("الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 18, 24, 24)
            setBackgroundColor(Color.rgb(247,247,251))
        }

        val title = TextView(this).apply {
            text = "عجلة أيام الأسبوع"
            textSize = 21f
            setTextColor(Color.rgb(32,32,42))
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, 50))

        current = TextView(this).apply {
            text = days[0]
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(32,32,42))
            gravity = Gravity.CENTER
        }
        root.addView(current, LinearLayout.LayoutParams(-1, 58))

        wheel = WeekWheelView(this)
        val wheelParams = LinearLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * .86f).toInt(),
            (resources.displayMetrics.widthPixels * .86f).toInt()
        )
        wheelParams.gravity = Gravity.CENTER
        root.addView(wheel, wheelParams)

        val button = Button(this).apply {
            text = "اليوم التالي"
            textSize = 17f
            setOnClickListener { nextDay() }
        }
        val bp = LinearLayout.LayoutParams(-2, 58)
        bp.gravity = Gravity.CENTER
        bp.topMargin = 8
        root.addView(button, bp)

        val hint = TextView(this).apply {
            text = "كل ضغطة تنقل إلى اليوم التالي مع صوت نقرة خفيف."
            textSize = 13f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        root.addView(hint, LinearLayout.LayoutParams(-1, 45))

        wheel.setOnClickListener { nextDay() }
        setContentView(root)
    }

    private fun nextDay() {
        wheel.advance {
            current.text = days[wheel.index]
        }
    }
}

class WeekWheelView(context: Context) : View(context) {
    private val days = arrayOf("الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت")
    var index = 0
        private set
    private var rotation = 0f
    private var animating = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(32,32,42)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(217,217,227)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(79,70,229)
        style = Paint.Style.FILL
    }
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 55)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * .455f
        val rect = RectF(cx-r, cy-r, cx+r, cy+r)

        canvas.save()
        canvas.rotate(rotation, cx, cy)

        for (i in 0 until 7) {
            val start = i * 360f / 7f - 180f / 7f
            paint.color = if (i % 2 == 0) Color.WHITE else Color.rgb(240,240,247)
            paint.style = Paint.Style.FILL
            canvas.drawArc(rect, start, 360f/7f, true, paint)
            canvas.drawArc(rect, start, 360f/7f, true, linePaint)

            val mid = Math.toRadians(i * 360.0 / 7.0 - 90.0)
            val tx = cx + r * .68f * cos(mid).toFloat()
            val ty = cy + r * .68f * sin(mid).toFloat() - (textPaint.ascent()+textPaint.descent())/2
            textPaint.textSize = if (days[i].length > 6) r*.09f else r*.105f
            canvas.drawText(days[i], tx, ty, textPaint)
        }

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, r*.25f, paint)
        linePaint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, r*.25f, linePaint)
        textPaint.textSize = r*.09f
        canvas.drawText("اضغط", cx, cy - (textPaint.ascent()+textPaint.descent())/2, textPaint)
        canvas.restore()

        // Fixed short arrow: its tip touches the middle of the selected sector's outer arc.
        val tipY = cy - r + 1f
        val baseY = tipY - r*.11f
        val half = r*.065f
        val path = Path().apply {
            moveTo(cx, tipY)
            lineTo(cx-half, baseY)
            lineTo(cx+half, baseY)
            close()
        }
        canvas.drawPath(path, pointerPaint)
    }

    fun advance(onFinished: () -> Unit) {
        if (animating) return
        animating = true
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 55)
        val old = rotation
        val target = old - 360f/7f
        index = (index + 1) % 7

        ValueAnimator.ofFloat(old, target).apply {
            duration = 650
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                rotation = it.animatedValue as Float
                invalidate()
            }
            doOnEnd {
                rotation = target % 360f
                animating = false
                onFinished()
                invalidate()
            }
            start()
        }
    }

    private fun ValueAnimator.doOnEnd(action: () -> Unit) {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) = action()
        })
    }
}
