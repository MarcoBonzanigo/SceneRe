package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.ScrollView


class SwipeButtonScrollView(context: Context, attributeSet: AttributeSet) : ScrollView(context, attributeSet) {

    private var locked = false
    private var lockedInternal = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return !lockedInternal && !locked && super.onTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        lockedInternal = false
        if (childCount == 1 && getChildAt(0) is LinearLayout) {
            val innerLinearLayout = getChildAt(0) as LinearLayout
            for (i in 0..innerLinearLayout.childCount) {
                if (innerLinearLayout.getChildAt(i) is SwipeButton && (innerLinearLayout.getChildAt(i) as SwipeButton).interacted) {
                    lockedInternal = true
                    return false
                }
            }
        }
        return !locked && super.onInterceptTouchEvent(ev)
    }
}