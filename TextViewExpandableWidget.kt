package com.lnsantos.textviewexpandablewidget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

class TextViewExpandableWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr){

    private var widgetState = TextViewExpandableWidgetState(0, true, 80f)
    private var callback : Listeners? = null

    init {
        onRecoverStyleAttributes(attrs)
        onRecoverFirstHeight()
        setOnClickListener {
            if (widgetState.changeStatusInText) {
                onChangeStateView()
            }
        }
    }

    fun setStateTextViewExpandable(callback : Listeners){
        this.callback = callback
    }

    private fun onRecoverFirstHeight() {
        viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    widgetState.oldHeight = height
                    viewTreeObserver.removeOnGlobalLayoutListener(this)

                    onForceStartHeight(widgetState.heightStart)
                }
            }
        )
    }

    fun onForceStartHeight(forceHeight: Float) {
        AnimatorSet().apply {
            play(resizeHeightViewAnimation(layoutParams.height, convertDpToPixel(forceHeight, context).toInt(),100))
            start()
        }
    }

    private fun onRecoverStyleAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val attributes = context.obtainStyledAttributes(it,
                R.styleable.TextViewExpandableWidget
            )
            widgetState.heightStart = attributes.getDimension(
                R.styleable.TextViewExpandableWidget_startHeight,
                80f
            )

            widgetState.changeStatusInText = attributes.getBoolean(
                R.styleable.TextViewExpandableWidget_changeClickInText,
                true
            )

            widgetState.durationExpandTimeInMiles = attributes.getInteger(
                R.styleable.TextViewExpandableWidget_durationExpandTimeInMiles,
                500
            ).toLong()

            widgetState.durationCollapseTimeInMiles = attributes.getInteger(
                R.styleable.TextViewExpandableWidget_durationCollapseTimeInMiles,
                500
            ).toLong()

            attributes.recycle()
        }
    }

    fun onChangeStateView() {
        if (widgetState.control) {
            widgetState.control = false
            callback?.onStateTextViewExpandable(true)
            onExpandView()
        } else {
            widgetState.control = true
            callback?.onStateTextViewExpandable(false)
            onCollapseView(widgetState.heightStart)
        }
    }

    private fun onCollapseView(heightStart: Float) {
        AnimatorSet().apply {
            playTogether(resizeHeightViewAnimation(layoutParams.height, convertDpToPixel(heightStart, context).toInt(),300))
            play(hideCollapseText(heightStart,widgetState.durationCollapseTimeInMiles))
            start()
        }
    }

    private fun onExpandView() {
        AnimatorSet().apply {
            playTogether(resizeHeightViewAnimation(layoutParams.height, widgetState.oldHeight,300))
            play(showExpandAllText(widgetState.durationExpandTimeInMiles))
            start()
        }
    }

    private fun hideCollapseText(dp: Float, duration: Long) =
        ObjectAnimator.ofFloat(this, View.ALPHA, 1f).apply {
            setDuration(duration)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)

                    val customLayoutParam = ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        convertDpToPixel(dp, context).toInt()
                    )
                    settingMargin(customLayoutParam)
                    layoutParams = customLayoutParam
                    requestLayout()
                }
            })
        }

    private fun settingMargin(customLayoutParam: ConstraintLayout.LayoutParams) {
        customLayoutParam.setMargins(marginLeft, marginTop, marginRight, marginBottom)

        if (Build.VERSION.SDK_INT > 16){
            customLayoutParam.marginEnd = marginEnd
            customLayoutParam.marginStart = marginStart
            customLayoutParam.updateMarginsRelative(marginStart, marginTop, marginEnd, marginBottom)
        }
    }

    private fun showExpandAllText(duration: Long) =
        ObjectAnimator.ofFloat(this, View.ALPHA, 1f).apply {
            setDuration(duration)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    super.onAnimationStart(animation)

                    val customLayoutParam = ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    settingMargin(customLayoutParam)
                    layoutParams = customLayoutParam
                    requestLayout()
                }
            })
        }

    private fun resizeHeightViewAnimation(
        oldHeight: Int,
        newHeight: Int,
        time: Long
    ) = ObjectAnimator.ofInt(oldHeight, newHeight).apply {
        duration = time
        interpolator = FastOutSlowInInterpolator()
        addUpdateListener {
            layoutParams.height = it.animatedValue as Int
            requestLayout()
        }
    }

    interface Listeners{
        fun onStateTextViewExpandable(isExpand: Boolean)
    }

    data class TextViewExpandableWidgetState(
        var oldHeight: Int = 0,
        var control : Boolean = true,
        var heightStart: Float = 80f,
        var changeStatusInText: Boolean = true,
        var durationExpandTimeInMiles: Long = 500,
        var durationCollapseTimeInMiles: Long = 500
    )

    fun convertDpToPixel(dp: Float, context: Context): Float {
        return dp * (context.resources
            .displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

}