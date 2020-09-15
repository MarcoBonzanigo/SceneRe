package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import uzh.scenere.R
import uzh.scenere.datamodel.IElement
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.triggers.communication.NfcTrigger
import uzh.scenere.datamodel.triggers.direct.IfElseTrigger
import uzh.scenere.datamodel.triggers.direct.StakeholderInteractionTrigger
import uzh.scenere.helpers.*
import uzh.scenere.views.SreTextView.TextStyle.DARK
import uzh.scenere.views.SreTextView.TextStyle.LIGHT
import java.io.Serializable


@SuppressLint("ViewConstructor")
class Element (context: Context, private var element: IElement, private val top: Boolean, private  var left: Boolean, private  var right: Boolean, private  val bottom: Boolean) : RelativeLayout(context), Serializable {

    var editButton: IconButton? = null
    var deleteButton: IconButton? = null
    var addButton: IconButton? = null
    var removeButton: IconButton? = null
    var whatIfButton: IconButton? = null
    var nfcButton: IconButton? = null
    var interactionView: SreTextView? = null
    var pathSpinner: SreSpinner? = null
    private var connectionTop: TextView? = null
    private var connectionLeft: TextView? = null
    private var connectionRight: TextView? = null
    private var connectionBottom: TextView? = null
    private var centerElement: SreTextView? = null
    private var topWrapper: RelativeLayout? = null
    private var centerWrapper: RelativeLayout? = null
    private var bottomWrapper: RelativeLayout? = null

    private var dpiConnectorWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
    private var dpiConnectorHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()

    var nfcDataLoaded:Boolean = false

    init {
        //PARENT
        layoutParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        //WRAPPER
        topWrapper = RelativeLayout(context)
        topWrapper?.id = View.generateViewId()
        centerWrapper = RelativeLayout(context)
        centerWrapper?.id = View.generateViewId()
        bottomWrapper = RelativeLayout(context)
        bottomWrapper?.id = View.generateViewId()
        //CHILDREN
        connectionTop = TextView(context)
        connectionLeft = TextView(context)
        connectionRight = TextView(context)
        connectionBottom = TextView(context)
        centerElement = SreTextView(context,centerWrapper,null,if (isStep()) DARK else LIGHT)
        //TEXT
        connectionTop?.textSize = 0f
        connectionLeft?.textSize = 0f
        connectionRight?.textSize = 0f
        connectionBottom?.textSize = 0f
        //TOP
        createTop()
        val topParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        topParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
        addView(topWrapper, topParams)
        //CENTER
        createCenter()
        val centerParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        centerParams.addRule(RelativeLayout.BELOW, NumberHelper.nvl(topWrapper?.id, 0))
        addView(centerWrapper, centerParams)
        //BOTTOM
        createBottom()
        val bottomParams = RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        bottomParams.addRule(RelativeLayout.BELOW, NumberHelper.nvl(centerWrapper?.id, 0))
        addView(bottomWrapper, bottomParams)
    }

    private fun createTop() {
        connectionTop?.id = View.generateViewId()
        // LEFT
        editButton = IconButton(context, topWrapper, R.string.icon_edit,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.LEFT_OF, connectionTop!!.id).addRule(CENTER_VERTICAL, TRUE)
        topWrapper?.addView(editButton)
        // CENTER
        if (top) {
            connectionTop?.setBackgroundColor(getColorWithStyle(context,R.color.sreBlack))
        }
        val centerParams = LayoutParams(dpiConnectorWidth, dpiConnectorHeight+NumberHelper.nvl(editButton?.getTopMargin(),0)*2)
        centerParams.addRule(CENTER_HORIZONTAL, TRUE)
        connectionTop?.layoutParams = centerParams
        topWrapper?.addView(connectionTop)
        // RIGHT
        deleteButton = IconButton(context,topWrapper, R.string.icon_delete,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.RIGHT_OF, connectionTop!!.id).addRule(CENTER_VERTICAL, TRUE)
        topWrapper?.addView(deleteButton)
    }

    private fun createCenter() {
        // CENTER
        centerElement?.id = View.generateViewId()
        centerElement?.addRule(CENTER_IN_PARENT, TRUE)
        centerWrapper?.addView(centerElement)
        // LEFT
        updateLeft()
        val leftParams = LayoutParams(dpiConnectorHeight, dpiConnectorWidth)
        leftParams.addRule(START_OF, NumberHelper.nvl(centerElement?.id, 0))
        leftParams.addRule(ALIGN_PARENT_START, TRUE)
        leftParams.addRule(CENTER_VERTICAL, TRUE)
        centerWrapper?.addView(connectionLeft, leftParams)
        // RIGHT
        updateRight()
        val rightParams = LayoutParams(dpiConnectorHeight, dpiConnectorWidth)
        rightParams.addRule(END_OF, NumberHelper.nvl(centerElement?.id, 0))
        rightParams.addRule(ALIGN_PARENT_END, TRUE)
        rightParams.addRule(CENTER_VERTICAL, TRUE)
        centerWrapper?.addView(connectionRight, rightParams)
    }

    private fun createBottom() {
        connectionBottom?.id = View.generateViewId()
        // LEFT
        pathSpinner = SreSpinner(context,bottomWrapper, emptyArray()).addRule(RelativeLayout.LEFT_OF, connectionBottom!!.id).addRule(CENTER_VERTICAL, TRUE)
        pathSpinner?.visibility = if (element is IfElseTrigger) View.VISIBLE else View.INVISIBLE
        bottomWrapper?.addView(pathSpinner)
        // CENTER
        if (bottom) {
            connectionBottom?.setBackgroundColor(getColorWithStyle(context,R.color.sreBlack))
        }
        connectionBottom?.layoutParams = connectionTop?.layoutParams
        bottomWrapper?.addView(connectionBottom)
        // RIGHT
        when (element){
            is IfElseTrigger -> {
                addButton = IconButton(context, bottomWrapper, R.string.icon_folder_plus,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.RIGHT_OF, connectionBottom!!.id).addRule(CENTER_VERTICAL, TRUE)
                addButton?.id = View.generateViewId()
                removeButton = IconButton(context, bottomWrapper, R.string.icon_folder_minus,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.RIGHT_OF, addButton!!.id).addRule(CENTER_VERTICAL, TRUE)
                bottomWrapper?.addView(addButton)
                bottomWrapper?.addView(removeButton)
            }
            is StakeholderInteractionTrigger -> {
                updateInteraction((element as StakeholderInteractionTrigger).interactedStakeholderId!!)
            }
            is NfcTrigger -> {
                nfcButton = IconButton(context, bottomWrapper, R.string.icon_nfc,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.RIGHT_OF, connectionBottom!!.id).addRule(CENTER_VERTICAL, TRUE)
                bottomWrapper?.addView(nfcButton)
                checkNfcEnabled()
            }
            is AbstractStep -> {
                whatIfButton = IconButton(context, bottomWrapper, R.string.icon_what_if,dpiConnectorHeight,dpiConnectorHeight).addRule(RelativeLayout.RIGHT_OF, connectionBottom!!.id).addRule(CENTER_VERTICAL, TRUE)
                bottomWrapper?.addView(whatIfButton)
            }
        }
    }

    fun updateLeft(l: Boolean = left): Element {
        left = l
        if (left) {
            connectionLeft?.setBackgroundColor(getColorWithStyle(context, R.color.sreBlack))
        } else {
            connectionLeft?.setBackgroundColor(getColorWithStyle(context, R.color.transparent))
        }
        return this
    }

    fun updateRight(r: Boolean = right): Element {
        right = r
        if (right) {
            connectionRight?.setBackgroundColor(getColorWithStyle(context, R.color.sreBlack))
        } else {
            connectionRight?.setBackgroundColor(getColorWithStyle(context, R.color.transparent))
        }
        return this
    }

    fun updateInteraction(stakeholderId: String): Element {
        val stakeholder = DatabaseHelper.getInstance(context).read(stakeholderId, Stakeholder::class)
        if (stakeholder !is Stakeholder.NullStakeholder) {
            if (interactionView != null) {
                bottomWrapper?.removeView(interactionView)
            }
            interactionView = SreTextView(context, bottomWrapper, stakeholder.name)
            if (right) {
                interactionView?.text = StringHelper.styleString(R.string.connect_right, context, arrayListOf(stakeholder.name))
                interactionView?.addRule(ALIGN_PARENT_END, connectionBottom!!.id)
                interactionView?.setMargin(0, DipHelper.get(resources).dip3, 0, 0)
            }
            if (left) {
                interactionView?.text = StringHelper.styleString(R.string.connect_left, context, ArrayList(), arrayListOf(stakeholder.name))
                interactionView?.addRule(ALIGN_PARENT_START, connectionBottom!!.id)
                interactionView?.setMargin(DipHelper.get(resources).dip3, 0, 0, 0)
            }
            bottomWrapper?.addView(interactionView?.addRule(CENTER_VERTICAL, TRUE))
        }
        return this
    }

    fun connectToNext(){
        connectionBottom?.setBackgroundColor(getColorWithStyle(context,R.color.sreBlack))
        deleteButton?.visibility = View.INVISIBLE
    }

    fun disconnectFromNext(){
        connectionBottom?.setBackgroundColor(Color.TRANSPARENT)
        deleteButton?.visibility = View.VISIBLE
    }

    fun withLabel(label: String?): Element{
        centerElement?.text = label
        return this
    }

    fun withLabel(label: Spanned?): Element{
        centerElement?.text = label
        return this
    }

    fun updateElement(element: IElement): Element{
        this.element = element
        return this
    }

    fun isStep(): Boolean {
        return element is AbstractStep
    }


    fun containsElement(element: IElement): Boolean {
        return this.element.getElementId() == element.getElementId()
    }

    fun setPathData(lookupData: Array<String>): Element{
        pathSpinner?.updateLookupData(lookupData)
        return this
    }

    fun setEditExecutable(function: () -> Unit): Element {
        editButton?.setExecutable(function)
        return this
    }

    fun setDeleteExecutable(function: () -> Unit): Element {
        deleteButton?.setExecutable(function)
        deleteButton?.setLongClickOnly(true)
        return this
    }

    fun setAddExecutable(function: () -> Unit): Element {
        addButton?.setExecutable(function)
        return this
    }

    fun setWhatIfExecutable(function: () -> Unit): Element {
        whatIfButton?.setExecutable(function)
        return this
    }

    fun setRemoveExecutable(function: () -> Unit): Element {
        removeButton?.setExecutable(function)
        return this
    }

    fun setOnPathIndexSelectedExecutable(executable: (Int, Any?) -> Unit): Element{
        pathSpinner?.setIndexExecutable(executable)
        pathSpinner?.setDataObject(element)
        return this
    }

    fun setOnPathTextSelectedExecutable(executable: (String, Any?) -> Unit): Element{
        pathSpinner?.setSelectionExecutable(executable)
        pathSpinner?.setDataObject(element)
        return this
    }

    fun setInitSelectionExecutable(executable: (text: String)-> Unit): Element{
        pathSpinner?.setInitSelectionExecutable(executable)
        return this
    }

    fun setNothingSelectedExecutable(executable: ()-> Unit): Element{
        pathSpinner?.setNothingSelectedExecutable(executable)
        return this
    }

    fun setNfcExecutable(executable: ()-> Unit): Element{
        nfcButton?.setExecutable(executable)
        return this
    }

    fun setNfcLoaded(loaded: Boolean) {
        nfcDataLoaded = loaded
        if (loaded){
            nfcButton?.setTextColor(getColorWithStyle(context, R.color.srePrimaryAttention))
        }else{
            nfcButton?.setTextColor(getColorWithStyle(context, R.color.srePrimaryPastel))
        }
    }

    fun checkNfcEnabled(): Boolean {
        val nfcEnabled = CommunicationHelper.supportsNfcAndEnabled(context, CommunicationHelper.Companion.Communications.NFC)
        if (!nfcEnabled){
            nfcButton?.setTextColor(getColorWithStyle(context, R.color.srePrimaryDisabled))
        }
        return nfcEnabled
    }

    fun resetSelectCount(): Element{
        pathSpinner?.selectCount = 0
        return this
    }

    fun setZebraPattern(enabled: Boolean = false){
        if (enabled){
            setBackgroundColor(getColorWithStyle(context,R.color.srePrimaryPastelZebra))
        }else{
            setBackgroundColor(getColorWithStyle(context,R.color.sreWhiteZebra))
        }
    }
}