package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup


@SuppressLint("ViewConstructor")
class IconTextView(context: Context, parent: ViewGroup?, label: String? = null, style: TextStyle = TextStyle.LIGHT) : SreTextView(context,parent,label,style) {

    init {
        typeface = Typeface.createFromAsset(context.assets, "FontAwesome900.otf")
    }

    override fun addRule(verb: Int, subject: Int?): IconTextView {
        return super.addRule(verb, subject) as IconTextView
    }

}