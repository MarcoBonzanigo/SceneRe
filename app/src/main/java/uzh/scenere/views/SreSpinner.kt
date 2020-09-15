package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.*
import uzh.scenere.R
import uzh.scenere.helpers.*

@SuppressLint("ViewConstructor")
class SreSpinner(context: Context, parent: ViewGroup?, lookupData: Array<String>): Spinner(context) {

    private var data: Array<String> = lookupData
    private var multiSelectionMap = HashMap<String,TextView>()
    private var selectionCarrier: LinearLayout? = null

    private var initSelectionExecutable: ((text: String)-> Unit?)? = null
    private var selectionExecutable: ((text: String, data: Any?)-> Unit?)? = null
    private var indexExecutable: ((index: Int, data: Any?)-> Unit?)? = null
    private var nothingSelectedExecutable: (()-> Unit)? = null
    private var dataObject: Any? = null
    private val viewResource = resolveSpinnerLayoutStyle(context)
    var selectCount = 0

    enum class ParentLayout{
        RELATIVE,LINEAR,FRAME,UNKNOWN

    }

    private var parentLayout: ParentLayout = if (parent is LinearLayout) ParentLayout.LINEAR else if (parent is RelativeLayout) ParentLayout.RELATIVE else if (parent is FrameLayout) ParentLayout.FRAME else ParentLayout.UNKNOWN

    init{
        updateLookupData(data)
        dropDownVerticalOffset = DipHelper.get(resources).dip3_5.toFloat().toInt()
        val padding = context.resources.getDimension(R.dimen.dpi5).toInt()

        val margin = context.resources.getDimension(R.dimen.dpi0).toInt()
        setPadding(padding,padding,padding,padding)
        when (parent) {
            is LinearLayout -> {
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,  LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            is RelativeLayout -> {
                val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
            is FrameLayout -> {
                val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                params.setMargins(margin,margin,margin,margin)
                layoutParams = params
            }
        }
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val spinnerText = selectedItem as String
                if (StringHelper.hasText(spinnerText) && selectionCarrier != null) {
                    addSpinnerSelection(spinnerText)
                }
                if (selectCount == 0){
                    if (initSelectionExecutable != null){
                        try{
                            initSelectionExecutable?.invoke(spinnerText)
                        }catch (e: Exception){/*NOP*/ }
                    }
                }else{
                    if (selectionExecutable != null){
                        try{
                            selectionExecutable?.invoke(spinnerText,dataObject)
                        }catch (e: Exception){/*NOP*/ }
                    }
                    if (indexExecutable != null){
                        try{
                            indexExecutable?.invoke(position,dataObject)
                        }catch (e: Exception){/*NOP*/ }
                    }
                }
                selectCount++
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                if (nothingSelectedExecutable != null){
                    try {
                        nothingSelectedExecutable?.invoke()
                    }catch (e: Exception){/*NOP*/ }
                }
            }
        }
    }

    fun updateLookupData(lookupData: Array<String>){
        this.data = lookupData
        val spinnerArrayAdapter = ArrayAdapter<String>(context, viewResource, lookupData)
        spinnerArrayAdapter.setDropDownViewResource(viewResource)
        adapter = spinnerArrayAdapter
    }

    fun setSelectionCarrierLayout(layout: LinearLayout){
        selectionCarrier = layout
    }

    fun setSelectionExecutable(executable: (String, Any?) -> Unit){
        selectionExecutable = executable
    }

    fun setIndexExecutable(executable: (Int, Any?) -> Unit){
        indexExecutable = executable
    }

    fun setInitSelectionExecutable(executable: (text: String)-> Unit){
        initSelectionExecutable = executable
    }

    fun setNothingSelectedExecutable(executable: ()-> Unit){
        nothingSelectedExecutable = executable
    }

    fun setDataObject(data: Any){
        dataObject = data
    }

    fun getSelectedValues(): Array<String>{
        if (selectionCarrier != null){
            val array = ArrayList<String>()
            for (entry in multiSelectionMap.entries){
                array.add(entry.key)
            }
            return array.toTypedArray()
        }
        return emptyArray()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSpinnerSelection(spinnerText: String) {
        if (selectionCarrier == null){
            return
        }
        val textView = SreTextView(context, selectionCarrier, spinnerText, SreTextView.TextStyle.DARK)
        val textParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val margin = context.resources.getDimension(R.dimen.dpi0).toInt()
        textParams.setMargins(margin,margin,margin,margin)
        textView.layoutParams = textParams
        textView.text = spinnerText
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        textView.setBackgroundColor(getColorWithStyle(context,R.color.sreWhite))
        textView.setTextColor(getColorWithStyle(context,R.color.sreBlack))
        textView.setPadding(margin,margin,margin,margin)
        textView.setOnTouchListener { _, _ ->
            selectionCarrier?.removeView(textView)
            multiSelectionMap.remove(spinnerText)
            false
        }
        selectionCarrier?.addView(textView)
        if (multiSelectionMap[spinnerText] == null) {
            multiSelectionMap[spinnerText] = textView
        }
        setSelection(0)
    }

    fun addRule(verb: Int, subject: Int? = null): SreSpinner {
        when (parentLayout){
            ParentLayout.RELATIVE -> {
                if (subject == null){
                    (layoutParams as RelativeLayout.LayoutParams).addRule(verb)
                }else{
                    (layoutParams as RelativeLayout.LayoutParams).addRule(verb,subject)
                }
            }
            else -> {}
        }
        return this
    }

}