package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import uzh.scenere.R
import java.io.Serializable
import java.lang.reflect.AccessibleObject.setAccessible



@SuppressLint("ViewConstructor")
class SreScrollView(context: Context, parent: ViewGroup): ScrollView(context), Serializable {
    
    private val contentHolder = LinearLayout(context)

    enum class ParentLayout{
        RELATIVE,LINEAR,FRAME,UNKNOWN
    }
    private var parentLayout: SreScrollView.ParentLayout = if (parent is LinearLayout) SreScrollView.ParentLayout.LINEAR else if (parent is RelativeLayout) SreScrollView.ParentLayout.RELATIVE else if (parent is FrameLayout) SreScrollView.ParentLayout.FRAME else SreScrollView.ParentLayout.UNKNOWN

    init {
        contentHolder.orientation = LinearLayout.VERTICAL
        contentHolder.gravity = Gravity.CENTER_HORIZONTAL
        addView(contentHolder)
        val padding = context.resources.getDimension(R.dimen.dpi0).toInt()
        val margin = context.resources.getDimension(R.dimen.dpi0).toInt()
        setPadding(padding,padding,padding,padding)

        when (parentLayout) {
            ParentLayout.LINEAR -> {
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,  LinearLayout.LayoutParams.MATCH_PARENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            ParentLayout.RELATIVE -> {
                val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            ParentLayout.FRAME -> {
                val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            ParentLayout.UNKNOWN -> {}
        }


        try {
            val scrollCacheField = View::class.java.getDeclaredField("mScrollCache")
            scrollCacheField.isAccessible = true
            val scrollCache = scrollCacheField.get(this)
            val scrollBarField = scrollCache.javaClass.getDeclaredField("scrollBar")
            scrollBarField.isAccessible = true
            val scrollBar = scrollBarField.get(scrollCache)
            val method = scrollBar.javaClass.getDeclaredMethod("setVerticalThumbDrawable", Drawable::class.java)
            method.isAccessible = true
            method.invoke(scrollBar, ContextCompat.getDrawable(context,R.drawable.sre_scrollbar))
            method.isAccessible = false
            isScrollbarFadingEnabled = false
        } catch (e: Exception) {
            //NOP
        }
    }

    fun addRule(verb: Int, subject: Int? = null): SreScrollView {
        when (parentLayout){
            SreScrollView.ParentLayout.RELATIVE -> {
                if (subject == null){
                    (layoutParams as RelativeLayout.LayoutParams).addRule(verb)
                }else{
                    (layoutParams as RelativeLayout.LayoutParams).addRule(verb,subject)
                }
            }
            else -> {}
        }
        return this
    }

    fun addScrollElement(view: View){
        contentHolder.addView(view)
    }

    fun removeScrollElement(view: View){
        contentHolder.removeView(view)
    }

    fun setScrollbarDrawable(drawable: Drawable){
        SreScrollView.setScrollbarDrawable(this,drawable)
    }

    companion object {
        fun setScrollbarDrawable(scrollView: ScrollView, drawable: Drawable){
            try {
                val mScrollCacheField = View::class.java.getDeclaredField("mScrollCache")
                mScrollCacheField.isAccessible = true
                val mScrollCache = mScrollCacheField.get(scrollView)

                val scrollBarField = mScrollCache.javaClass.getDeclaredField("scrollBar")
                scrollBarField.isAccessible = true
                val scrollBar = scrollBarField.get(mScrollCache)

                val method = scrollBar.javaClass.getDeclaredMethod("setVerticalThumbDrawable", Drawable::class.java)
                method.isAccessible = true

                method.invoke(scrollBar, drawable)
            } catch (e: Exception) {
                //NOP
            }
        }
    }

}