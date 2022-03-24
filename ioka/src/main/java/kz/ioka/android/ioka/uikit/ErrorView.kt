package kz.ioka.android.ioka.uikit

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.coroutineScope
import kz.ioka.android.ioka.R


internal class ErrorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr), LifecycleObserver {

    private lateinit var tvErrorText: AppCompatTextView
    private lateinit var btnClose: AppCompatImageButton

    private lateinit var scope: LifecycleCoroutineScope

    private val visibilityHandler = Handler(Looper.getMainLooper())
    private val callback = Runnable {
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.duration = 200
        this.animation = fadeOut

        isVisible = false
    }

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.view_error, this, true)

        bindViews(root)
        background = AppCompatResources.getDrawable(context, R.drawable.bg_error)
        orientation = HORIZONTAL
        isVisible = false

        btnClose.setOnClickListener { hide() }
    }

    private fun bindViews(root: View) {
        tvErrorText = root.findViewById(R.id.tvErrorText)
        btnClose = root.findViewById(R.id.btnClose)
    }

    fun registerLifecycleOwner(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
        scope = lifecycle.coroutineScope
    }

    fun show(text: String? = null) {
        text?.let { tvErrorText.text = text }

        isVisible = true

        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = 200
        this.animation = fadeIn

        visibilityHandler.postDelayed(callback, 3000)
    }

    fun hide() {
        visibilityHandler.removeCallbacks(callback)
        visibilityHandler.post(callback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        visibilityHandler.removeCallbacks(callback)
        this.animation = null
    }

}