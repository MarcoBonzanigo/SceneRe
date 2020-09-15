package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import uzh.scenere.R
import uzh.scenere.helpers.DipHelper
import uzh.scenere.helpers.NumberHelper
import java.io.Serializable

@SuppressLint("ViewConstructor")
class SreLoadingBar(context: Context, parent: ViewGroup?, height: Int = DipHelper.get(context.resources).dip5): LinearLayout(context), ISreView, Serializable {
    override var parentLayout =  ISreView.ParentLayout.UNKNOWN

    override fun getView(): View {
        return this
    }

    private var progress: LinearLayout

    init{
        resolveParent(parent)
        val params = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height)
        val margin = DipHelper.get(context.resources).dip5
        params.setMargins(margin,margin,margin,margin)
        layoutParams = params
        orientation = HORIZONTAL
        background = ContextCompat.getDrawable(context, R.drawable.sre_progress_bar_background)
        weightSum = 100f
        progress = LinearLayout(context)
        progress.layoutParams = LinearLayout.LayoutParams(0,LayoutParams.MATCH_PARENT,0f)
        progress.orientation = VERTICAL
        progress.background = ContextCompat.getDrawable(context, R.drawable.sre_progress_bar)
        addView(progress)
        adaptStyle(context)
    }

    fun setProgress(percent: Float){
        WeightAnimator(progress, NumberHelper.capAt(percent,0f,100f), 250).play()
    }
}