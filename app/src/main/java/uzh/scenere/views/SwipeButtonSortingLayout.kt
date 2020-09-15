package uzh.scenere.views

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import uzh.scenere.helpers.NumberHelper
import java.util.*

class SwipeButtonSortingLayout(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

    private var lastAddedSwipeButton: SwipeButton? = null

    init {
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun sort() {
        val treeMap: TreeMap<String, SwipeButton> = TreeMap()
        for (child in 0 until childCount) {
            if (getChildAt(child) is SwipeButton) {
                val swipeButton = getChildAt(child) as SwipeButton
                if (swipeButton.getText() != null) {
                    treeMap[swipeButton.getText()!!] = swipeButton
                }
            }
        }
        removeAllViews()
        for (entry in treeMap.entries) {
            addView(entry.value)
        }
    }

    override fun addView(newChild: View?) {
        if (newChild is SwipeButton) {
            lastAddedSwipeButton = newChild
            if (newChild.isFirstPosition()){
                return addView(newChild,0)
            }
            val label = newChild.getText() ?: return super.addView(newChild)
            for (c in 0 until childCount) {
                val child = getChildAt(c)
                if (child is SwipeButton && !child.isFirstPosition() && NumberHelper.nvl(child.getText()?.compareTo(label), -1) >= 0) {
                    return addView(newChild, c)
                }
            }
        }
        return super.addView(newChild)
    }

    fun scrollToLastAdded(): Boolean{
        if (lastAddedSwipeButton != null){
            Handler().postDelayed({requestChildFocus(lastAddedSwipeButton,lastAddedSwipeButton)},250)
            return true
        }
        return false
    }

    fun scrollTo(button: SwipeButton) {
        Handler().postDelayed({ requestChildFocus(button, button)}, 250)
    }
}