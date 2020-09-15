package uzh.scenere.helpers

import android.content.Context
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.ICON_PATTERN
import android.graphics.PorterDuff
import android.graphics.drawable.*
import android.widget.ImageView
import android.widget.ScrollView
import uzh.scenere.views.SreScrollView


class StyleHelper private constructor(context: Context) {

    val switchToNormalMode = HashMap<Int,Int>()
    val switchToContrastMode = HashMap<Int,Int>()
    val switchToOledMode = HashMap<Int,Int>()

    init {
        addEntry(context,R.color.sreWhite,R.color.sreWhite,R.color.sreWhite)
        addEntry(context,R.color.sreWhiteZebra,R.color.sreGray,R.color.sreOLEDBlack)
        addEntry(context,R.color.srePrimaryPastelZebra,R.color.sreBlack_II,R.color.sreBlack_II)
        addEntry(context,R.color.srePrimaryPastel,R.color.sreBlack_II,R.color.sreWhite_III)
        addEntry(context,R.color.srePrimaryLight,R.color.sreBlack_III,R.color.sreWhite_III)
        addEntry(context,R.color.srePrimary,R.color.sreBlack_IV,R.color.sreWhite_III)
        addEntry(context,R.color.srePrimaryDark,R.color.sreBlack_V,R.color.sreWhite_III)
        addEntry(context,R.color.sreBlack,R.color.sreWhite_III,R.color.sreWhite_III)
        addEntry(context,R.color.srePrimaryAttention,R.color.srePrimaryAttention,R.color.srePrimaryAttention)
        addEntry(context,R.color.srePrimaryWarn,R.color.srePrimaryWarn,R.color.srePrimaryWarn)
        addEntry(context,R.color.srePrimaryDisabledText,R.color.srePrimaryDisabledText,R.color.srePrimaryDisabledText)
        addEntry(context,R.color.srePrimaryDisabled,R.color.srePrimaryDisabled,R.color.srePrimaryDisabled)
    }

    private fun addEntry(context: Context, normalId: Int, contrastId: Int, oledId: Int){
        val normal = ContextCompat.getColor(context, normalId)
        val contrast = ContextCompat.getColor(context, contrastId)
        val oled = ContextCompat.getColor(context, oledId)
        switchToNormalMode[oled] = normal
        switchToNormalMode[oledId] = normal
        switchToContrastMode[normal] = contrast
        switchToContrastMode[normalId] = contrast
        switchToOledMode[normal] = oled
        switchToOledMode[normalId] = oled
    }

    fun switchColors(context: Context, view: View, toStyle: SreStyle){
        if (view is TextView) {
            var textColor: Int? = null
            var hintColor: Int? = null
            when (toStyle) {
                SreStyle.NORMAL -> {
                    textColor = switchToNormalMode[view.currentTextColor]
                    hintColor = switchToNormalMode[view.currentHintTextColor]
                    if (StringHelper.hasText(view.text) && !view.text.matches(ICON_PATTERN)) {
                        //no icons contained
                        view.typeface = Typeface.DEFAULT
                    }
                }
                SreStyle.CONTRAST-> {
                    textColor = switchToContrastMode[view.currentTextColor]
                    hintColor = switchToContrastMode[view.currentHintTextColor]
                    if (StringHelper.hasText(view.text) && !view.text.matches(ICON_PATTERN)) {
                        //no icons contained
                        view.typeface = Typeface.DEFAULT_BOLD
                    }
                }
                SreStyle.OLED -> {
                    textColor = switchToOledMode[view.currentTextColor]
                    hintColor = switchToOledMode[view.currentHintTextColor]
                    if (textColor == null){
                        textColor = ContextCompat.getColor(context,R.color.sreWhite)
                    }
                    if (hintColor == null){
                        hintColor = ContextCompat.getColor(context,R.color.sreWhite)
                    }
                    if (StringHelper.hasText(view.text) && !view.text.matches(ICON_PATTERN)) {
                        //no icons contained
                        view.typeface = Typeface.DEFAULT_BOLD
                    }
                }
            }
            if (textColor != null) {
                view.setTextColor(textColor)
            }
            if (hintColor != null) {
                view.setHintTextColor(hintColor)
            }
        }else if (view is ImageView  && view.drawable != null && toStyle == SreStyle.OLED){
            view.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sre_logo_oled))
        }else if (view is ImageView  && view.drawable != null && toStyle != SreStyle.OLED){
            view.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.sre_logo))
        }else if (view is ScrollView){
            if (toStyle == SreStyle.OLED){
                SreScrollView.setScrollbarDrawable(view,ContextCompat.getDrawable(context,R.drawable.sre_scrollbar_oled))
            }else{
                SreScrollView.setScrollbarDrawable(view,ContextCompat.getDrawable(context,R.drawable.sre_scrollbar))
            }
        }
        if (toStyle == SreStyle.OLED){
            val background = view.background
            if (background != null){
                var color: Int? = null
                if (background is ColorDrawable){
                    view.setBackgroundColor(ContextCompat.getColor(context,R.color.sreOLEDBlack))
                }else if (background is InsetDrawable){
                    background.drawable.setColorFilter(ContextCompat.getColor(context, R.color.sreOLEDBlack), PorterDuff.Mode.MULTIPLY)
                }else if (background is GradientDrawable){
                    background.setColorFilter(ContextCompat.getColor(context, R.color.sreOLEDBlack), PorterDuff.Mode.MULTIPLY)
                }else if (background is RippleDrawable){
                    background.setColorFilter(ContextCompat.getColor(context, R.color.sreOLEDBlack), PorterDuff.Mode.MULTIPLY)
                }
            }
        }
    }

    fun getStatusBarColor(context: Context, style: SreStyle): Int {
        if (style == SreStyle.OLED){
            return ContextCompat.getColor(context,R.color.sreBlack)
        }else{
            return ContextCompat.getColor(context,R.color.srePrimary)
        }
    }

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: StyleHelper? = null

        fun get(context: Context): StyleHelper {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = StyleHelper(context)
                    }
                    instance!!
                }
            }
        }
    }
}