package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.getColorWithStyle
import uzh.scenere.views.SreTextView.TextStyle.*
import java.io.Serializable

@SuppressLint("ViewConstructor")
open class SreTextView(context: Context, parent: ViewGroup?, label: String? = null, val style: TextStyle = LIGHT): TextView(context), ISreView, Serializable {

    constructor(context: Context, parent: ViewGroup?, stringId: Int, TextStyle: TextStyle = LIGHT): this(context,parent,context.getString(stringId),TextStyle)

    override var parentLayout = ISreView.ParentLayout.UNKNOWN

    override fun getView(): View {
        return this
    }

    init {
        text = label
        create(context,parent)
        adaptStyle(context)
    }

    enum class TextStyle{
        DARK,LIGHT,MEDIUM,ATTENTION,BORDERLESS_DARK, BORDERLESS_LIGHT
    }

    private fun create(context: Context, parent: ViewGroup?) {
        resolveParent(parent)
        id = View.generateViewId()
        gravity = Gravity.CENTER
        when (style){
            LIGHT, BORDERLESS_LIGHT -> {
                background = context.getDrawable(if (style== LIGHT) R.drawable.sre_text_view_light else R.drawable.sre_text_view_light_borderless)
                setTextColor(getColorWithStyle(context,R.color.srePrimaryDark))
            }
            DARK, BORDERLESS_DARK -> {
                background = context.getDrawable(if (style== DARK) R.drawable.sre_text_view_dark else R.drawable.sre_text_view_dark_borderless)
                setTextColor(getColorWithStyle(context,R.color.srePrimaryPastel))
            }
            MEDIUM -> {
                background = context.getDrawable(R.drawable.sre_text_view_medium)
                setTextColor(getColorWithStyle(context,R.color.srePrimaryPastel))
            }
            ATTENTION -> {
                background = context.getDrawable(R.drawable.sre_text_view_attention)
                setTextColor(getColorWithStyle(context,R.color.sreBlack))
            }
        }
        val padding = DipHelper.get(resources).dip15.toInt()
        val margin = DipHelper.get(resources).dip0.toInt()
        setPadding(padding,padding,padding,padding)
        when (parentLayout) {
            ISreView.ParentLayout.RELATIVE -> {
                val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            ISreView.ParentLayout.LINEAR -> {
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,  LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            ISreView.ParentLayout.FRAME -> {
                val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            ISreView.ParentLayout.UNKNOWN -> {}
        }
    }

    override fun addRule(verb: Int, subject: Int?): SreTextView {
        return super.addRule(verb, subject) as SreTextView
    }
}