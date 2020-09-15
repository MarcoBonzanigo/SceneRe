package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup

@SuppressLint("ViewConstructor")
class IconButton(context: Context, parent: ViewGroup?, iconId: Int, height: Int? = null, width: Int? = null) : SreButton(context,parent,iconId,height,width) {

    init {
        typeface = Typeface.createFromAsset(context.assets, "FontAwesome900.otf")
    }

    override fun addRule(verb: Int, subject: Int?): IconButton {
        return super.addRule(verb, subject) as IconButton
    }
}