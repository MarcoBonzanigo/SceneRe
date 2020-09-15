package uzh.scenere.views

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import uzh.scenere.helpers.SreStyle
import uzh.scenere.helpers.StyleHelper
import uzh.scenere.helpers.getSreStyle
import uzh.scenere.helpers.reStyle
import uzh.scenere.views.ISreView.ParentLayout.*

interface ISreView {

    enum class ParentLayout{
        RELATIVE,LINEAR,FRAME,UNKNOWN
    }

    var parentLayout: ParentLayout

    fun getView(): View

    fun resolveParent(parent: ViewGroup?){
        when (parent){
            is LinearLayout -> parentLayout = LINEAR
            is RelativeLayout -> parentLayout = RELATIVE
            is FrameLayout -> parentLayout = FRAME
            else -> ParentLayout.UNKNOWN
        }
    }

    fun setMargin(margin: Int){
        setMargin(margin,margin,margin,margin)
    }

    fun setMargin(l: Int,r: Int,b: Int,t: Int){
        when (parentLayout){
            LINEAR -> {
                (getView().layoutParams as LinearLayout.LayoutParams).setMargins(l,t,r,b)
            }
            RELATIVE -> {
                (getView().layoutParams as RelativeLayout.LayoutParams).setMargins(l,t,r,b)
            }
            FRAME -> {
                (getView().layoutParams as FrameLayout.LayoutParams).setMargins(l,t,r,b)
            }
            else -> {}
        }
    }

    open fun addRule(verb: Int, subject: Int? = null): View {
        when (parentLayout){
            RELATIVE -> {
                if (subject == null){
                    (getView().layoutParams as RelativeLayout.LayoutParams).addRule(verb)
                }else{
                    (getView().layoutParams as RelativeLayout.LayoutParams).addRule(verb,subject)
                }
            }
            else -> {}
        }
        return getView()
    }


    fun setWeight(weight: Float,  horizontal: Boolean = true, alternateProportion: Int = LinearLayout.LayoutParams.WRAP_CONTENT){
        when (parentLayout){
            LINEAR -> {
                val params = LinearLayout.LayoutParams(if (horizontal) LinearLayout.LayoutParams.MATCH_PARENT else alternateProportion,
                        if (horizontal) alternateProportion else LinearLayout.LayoutParams.MATCH_PARENT)
                params.weight = weight
                getView().layoutParams = params
            }
            else -> {}
        }
    }

    fun setWeight(layoutParamsWithWeight: LinearLayout.LayoutParams){
        when (parentLayout){
            LINEAR -> {
                getView().layoutParams = layoutParamsWithWeight
            }
            else -> {}
        }
    }

    fun setSize(height: Int,width: Int){
        when (parentLayout){
            LINEAR -> {
                (getView().layoutParams as LinearLayout.LayoutParams).height = height
                (getView().layoutParams as LinearLayout.LayoutParams).width = width
            }
            RELATIVE -> {
                (getView().layoutParams as RelativeLayout.LayoutParams).height = height
                (getView().layoutParams as RelativeLayout.LayoutParams).width = width
            }
            else -> {}
        }
    }

    fun setPadding(padding: Int){
        getView().setPadding(padding,padding,padding,padding)
    }

    fun getLeftMargin(): Int{
        return when (parentLayout){
            RELATIVE -> (getView().layoutParams as RelativeLayout.LayoutParams).leftMargin
            LINEAR -> (getView().layoutParams as LinearLayout.LayoutParams).leftMargin
            FRAME -> (getView().layoutParams as FrameLayout.LayoutParams).leftMargin
            else -> 0
        }
    }
    fun getRightMargin(): Int{
        return when (parentLayout){
            RELATIVE -> (getView().layoutParams as RelativeLayout.LayoutParams).rightMargin
            LINEAR -> (getView().layoutParams as LinearLayout.LayoutParams).rightMargin
            FRAME -> (getView().layoutParams as FrameLayout.LayoutParams).rightMargin
            else -> 0
        }
    }
    fun getTopMargin(): Int{
        return when (parentLayout){
            RELATIVE -> (getView().layoutParams as RelativeLayout.LayoutParams).topMargin
            LINEAR -> (getView().layoutParams as LinearLayout.LayoutParams).topMargin
            FRAME -> (getView().layoutParams as FrameLayout.LayoutParams).topMargin
            else -> 0
        }
    }
    fun getBottomMargin(): Int{
        return when (parentLayout){
            RELATIVE -> (getView().layoutParams as RelativeLayout.LayoutParams).bottomMargin
            LINEAR -> (getView().layoutParams as LinearLayout.LayoutParams).bottomMargin
            FRAME -> (getView().layoutParams as FrameLayout.LayoutParams).bottomMargin
            else -> 0
        }
    }

    fun adaptStyle(context: Context){
        ISreView.adaptStyle(context,getView())
    }

    companion object {
        fun adaptStyle(context: Context, view: View){
            val sreStyle = getSreStyle(context)
            if (sreStyle != SreStyle.NORMAL){
                StyleHelper.get(context).switchColors(context,view,sreStyle)
            }
        }
        fun adaptStyle(context: Context, view: ViewGroup){
            val sreStyle = getSreStyle(context)
            if (sreStyle != SreStyle.NORMAL){
                reStyle(context,view,sreStyle)
            }
        }
    }

}