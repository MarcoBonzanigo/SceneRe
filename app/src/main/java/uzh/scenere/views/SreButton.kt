package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import uzh.scenere.R
import uzh.scenere.helpers.SreStyle
import uzh.scenere.helpers.StyleHelper
import uzh.scenere.helpers.getColorWithStyle
import uzh.scenere.helpers.getSreStyle
import java.io.Serializable

@SuppressLint("ViewConstructor")
open class SreButton(context: Context, parent: ViewGroup?, label: String?, height: Int? = null, width: Int? = null, val style: ButtonStyle = ButtonStyle.LIGHT): Button(context), ISreView, Serializable {

    constructor(context: Context, parent: ViewGroup?, stringId: Int, height: Int? = null, width: Int? = null): this(context,parent,context.getString(stringId),height,width)

    override var parentLayout = ISreView.ParentLayout.UNKNOWN

    override fun getView(): View {
        return this
    }

    init {
        text = label
        create(context, parent, height, width)
        adaptStyle(context)
    }

    enum class ButtonStyle{
        LIGHT, DARK, ATTENTION, WARN, TUTORIAL
    }

    private lateinit var function: () -> Unit
    private var longClickOnly: Boolean = false
    var data: Any? = null //Carrier Object

    private fun create(context: Context, parent: ViewGroup?, height: Int?, width: Int?) {
        resolveParent(parent)
        setAllCaps(false)
        id = View.generateViewId()
        gravity = Gravity.CENTER
        resolveStyle(true)
        val padding = context.resources.getDimension(R.dimen.dpi5).toInt()
        val margin = context.resources.getDimension(R.dimen.dpi5).toInt()
        setPadding(padding, padding, padding, padding)
        when (parentLayout){

            ISreView.ParentLayout.RELATIVE -> {
                val params = RelativeLayout.LayoutParams(width
                        ?: RelativeLayout.LayoutParams.WRAP_CONTENT, height
                        ?: RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin, margin, margin, margin)
                layoutParams = params
            }
            ISreView.ParentLayout.LINEAR -> {
                val params = LinearLayout.LayoutParams(width
                        ?: LinearLayout.LayoutParams.WRAP_CONTENT, height
                        ?: LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin, margin, margin, margin)
                layoutParams = params
            }
            ISreView.ParentLayout.FRAME -> {}
            ISreView.ParentLayout.UNKNOWN -> {}
        }
        setOnTouchListener { _, event ->
            when (event?.action) {
                ACTION_DOWN -> alpha = 0.5f
                ACTION_UP, ACTION_CANCEL, ACTION_OUTSIDE -> alpha = 1.0f
            }
            false
        }
        setOnClickListener { _ ->
            execAction()
        }
        setOnLongClickListener {
            execAction(true)
            false
        }
    }

    private fun resolveStyle(enabled: Boolean) {
        if (enabled){
            when (style) {
                ButtonStyle.LIGHT -> {
                    background = context.getDrawable(R.drawable.sre_button)
                    setTextColor(getColorWithStyle(context, R.color.srePrimaryPastel))
                }
                ButtonStyle.DARK -> {
                    background = context.getDrawable(R.drawable.sre_button_dark)
                    setTextColor(getColorWithStyle(context, R.color.srePrimaryPastel))
                }
                ButtonStyle.ATTENTION -> {
                    background = context.getDrawable(R.drawable.sre_button_attention)
                    setTextColor(getColorWithStyle(context, R.color.sreBlack))
                }
                ButtonStyle.WARN -> {
                    background = context.getDrawable(R.drawable.sre_button_warn)
                    setTextColor(getColorWithStyle(context, R.color.sreBlack))
                }
                ButtonStyle.TUTORIAL -> {
                    background = context.getDrawable(R.drawable.sre_button_tutorial)
                    setTextColor(getColorWithStyle(context, R.color.sreBlack))
                }
            }
        }else{
            background = context.getDrawable(R.drawable.sre_button_disabled)
            setTextColor(getColorWithStyle(context,R.color.srePrimaryDisabledText))
        }
    }

    fun setExecutable(function: () -> Unit): SreButton {
        this.function = function
        return this
    }

    fun setLongClickOnly(longClick: Boolean) {
        this.longClickOnly = longClick
    }

    private fun execAction(longClick: Boolean = false) {
        if (longClickOnly && !longClick) {
            return
        }
        try {
            function()
        } catch (e: Exception) {
            val a = 1
            //NOP
        }
    }

    override fun showContextMenu(): Boolean {
        //NOP
        return false
    }

    override fun showContextMenu(x: Float, y: Float): Boolean {
        //NOP
        return false
    }

    override fun setEnabled(enabled: Boolean) {
        resolveStyle(enabled)
        super.setEnabled(enabled)
    }

    override fun addRule(verb: Int, subject: Int?): SreButton {
        return super.addRule(verb, subject) as SreButton
    }
}