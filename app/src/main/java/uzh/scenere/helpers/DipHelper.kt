package uzh.scenere.helpers

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout

class DipHelper private constructor(private val resources: Resources) {

    val dip0 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0f, resources.displayMetrics).toInt()
    val dip1 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt()
    val dip2 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
    val dip2_5 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.5f, resources.displayMetrics).toInt()
    val dip3 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics).toInt()
    val dip3_5 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3.5f, resources.displayMetrics).toInt() //Text
    val dip4 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
    val dip5 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()
    val dip6 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics).toInt()
    val dip10 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
    val dip15 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, resources.displayMetrics).toInt()
    val dip20 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, resources.displayMetrics).toInt()
    val dip30 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, resources.displayMetrics).toInt()
    val dip40 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics).toInt()
    val dip50 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()

    fun setPadding(view: View, padding: Float) {
        val dip = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, padding, resources.displayMetrics).toInt()
        view.setPadding(dip, dip, dip, dip)
    }
    fun setMargin(layoutParams: RelativeLayout.LayoutParams, margin: Float) {
        val dip = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, margin, resources.displayMetrics).toInt()
        layoutParams.setMargins(dip, dip, dip, dip)
    }
    fun setMargin(layoutParams: LinearLayout.LayoutParams, margin: Float) {
        val dip = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, margin, resources.displayMetrics).toInt()
        layoutParams.setMargins(dip, dip, dip, dip)
    }

    fun get(resourceId: Int): Float{
        return resources.getDimension(resourceId)
    }

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: DipHelper? = null

        fun get(resources: Resources): DipHelper {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = DipHelper(resources)
                    }
                    instance!!
                }
            }
        }
    }
}