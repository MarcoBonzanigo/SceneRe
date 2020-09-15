package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getColorWithStyle
import uzh.scenere.views.SreEditText.EditStyle.DARK
import uzh.scenere.views.SreEditText.EditStyle.LIGHT
import java.io.Serializable


@SuppressLint("ViewConstructor")
open class SreEditText(context: Context, parent: ViewGroup?, text: String? = null, hint: String? = null, val style: EditStyle = DARK) : EditText(context), ISreView, Serializable {

    override var parentLayout = ISreView.ParentLayout.UNKNOWN

    override fun getView(): View {
        return this
    }

    init {
        setHint(StringHelper.applyFilters(hint,context))
        setText(text)
        create(context, parent)
        adaptStyle(context)
    }

    enum class EditStyle {
        DARK, LIGHT
    }

    private fun create(context: Context, parent: ViewGroup?) {
        resolveParent(parent)
        textSize = DipHelper.get(resources).dip3_5.toFloat()
        id = View.generateViewId()
        gravity = Gravity.CENTER
        when (style) {
            DARK -> {
                background = context.getDrawable(R.drawable.sre_edit_text_dark)
                val color = getColorWithStyle(context, R.color.srePrimaryPastel)
                setTextColor(color)
                setHintTextColor(color)
            }
            LIGHT -> {
                background = context.getDrawable(R.drawable.sre_edit_text_light)
                val color = getColorWithStyle(context, R.color.srePrimaryDark)
                setTextColor(color)
                setHintTextColor(color)
            }
        }
        val padding = DipHelper.get(resources).dip15.toInt()
        val margin = DipHelper.get(resources).dip0.toInt()
        setPadding(padding, padding, padding, padding)
        when (parentLayout) {
            ISreView.ParentLayout.RELATIVE -> {
                val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin, margin, margin, margin)
                layoutParams = params
            }
            ISreView.ParentLayout.LINEAR -> {
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin, margin, margin, margin)
                layoutParams = params
            }
            ISreView.ParentLayout.FRAME -> {
                val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin, margin, margin, margin)
                layoutParams = params
            }
            ISreView.ParentLayout.UNKNOWN -> {
            }
        }
    }

    override fun addRule(verb: Int, subject: Int?): SreEditText {
        return super.addRule(verb, subject) as SreEditText
    }
}