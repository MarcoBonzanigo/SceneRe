package uzh.scenere.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import kotlinx.android.synthetic.main.scroll_holder.*
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.COMPLETE_REMOVAL_DISABLED
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.READ_ONLY
import uzh.scenere.const.Constants.Companion.SIMPLE_LOOKUP
import uzh.scenere.const.Constants.Companion.SINGLE_SELECT
import uzh.scenere.const.Constants.Companion.SINGLE_SELECT_WITH_PRESET_POSITION
import uzh.scenere.datamodel.*
import uzh.scenere.helpers.*
import uzh.scenere.views.*
import uzh.scenere.views.SreTextView.TextStyle.BORDERLESS_DARK
import uzh.scenere.views.SreTextView.TextStyle.MEDIUM
import java.util.*
import android.content.res.Configuration
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout.VERTICAL
import uzh.scenere.const.Constants.Companion.COMPLETE_REMOVAL_DISABLED_WITH_PRESET
import uzh.scenere.const.Constants.Companion.SEMI_COLON
import uzh.scenere.const.Constants.Companion.ZERO_S
import java.io.Serializable
import kotlin.collections.ArrayList


abstract class AbstractManagementActivity : AbstractBaseActivity() {

    enum class LockState {
        LOCKED, UNLOCKED
    }

    enum class ManagementMode {
        VIEW, EDIT_CREATE, OBJECTS, EDITOR
    }

    protected val inputMap: HashMap<String, TextView> = HashMap()
    protected val uncheckedMap: HashMap<String, TextView> = HashMap()
    protected val multiInputMap: HashMap<String, ArrayList<TextView>> = HashMap()
    protected var lockState: LockState = LockState.LOCKED
    protected var creationButton: SwipeButton? = null
    protected var activeButton: SwipeButton? = null
    private var scrollY: Int? = null

    //*********
    //* REACT *
    //*********
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        collapseAndRefreshAllButtons()
    }

    override fun onToolbarLeftClicked() { //SAVE
        if (isInputOpen()) {
            if (!execDoAdditionalCheck()) {
                return
            }
            for (entry in inputMap) {
                if (!StringHelper.hasText(entry.value.text)) {
                    notify(getString(R.string.warn_not_all_required_info_entered))
                    return
                }
            }
            createEntity()
            if (isSpacingEnabled()) {
                createTitle("", getContentHolderLayout())
            }
            if (getContentWrapperLayout() is SwipeButtonScrollView) {
                if (isInEditMode() && !isInAddMode()){
                    execScrollBack()
                }else{
                    execScroll()
                }
            }
            onToolbarRightClicked()
        } else {
            super.onBackPressed()
        }
    }

    open fun getIsFirstScrollUp(): Boolean {
        return true
    }

    override fun onBackPressed() {
        if (isInputOpen()){
            onToolbarRightClicked()
        }else if (getIsFirstScrollUp() && getContentWrapperLayout() is SwipeButtonScrollView &&
                (getContentWrapperLayout() as SwipeButtonScrollView).scrollY > 0){
            execFullScrollUp()
        }else{
            super.onBackPressed()
        }
    }

    override fun onToolbarCenterClicked() { //LOCK & UNLOCK
        if (isInViewMode()) {
            adaptToolbarText(null, null, changeLockState(), null, null)
            for (v in 0 until getContentHolderLayout().childCount) {
                if (getContentHolderLayout().getChildAt(v) is SwipeButton) {
                    (getContentHolderLayout().getChildAt(v) as SwipeButton).setButtonStates(lockState == LockState.UNLOCKED, true, true, true).updateViews(false)
                }
            }
        }
    }

    override fun onToolbarRightClicked() { //CLOSE
        if (isInputOpen()) {
            execMorphInfoBar(InfoState.MINIMIZED)
            val styleString = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
            getInfoTitle().text = styleString
            getInfoContent().text = ""
            resetToolbar()
            resetEditMode()
            activeButton?.collapse()
            activeButton = null
            execScrollBack()
        }
    }

    open fun execScroll(scrollBackToPrevious: Boolean = false) {
        if (scrollBackToPrevious){
            execScrollBack()
        }else{
            execFullScroll()
        }
    }

    private fun execFullScroll() {
        if (getContentHolderLayout() is SwipeButtonSortingLayout) {
            execMinimizeKeyboard()
            val alreadyLoaded = (getContentHolderLayout() as SwipeButtonSortingLayout).scrollToLastAdded()
            execMinimizeKeyboard()

            if (!alreadyLoaded || (getContentWrapperLayout() as SwipeButtonScrollView).scrollY == 0) {
                Handler().postDelayed({ (getContentWrapperLayout() as SwipeButtonScrollView).fullScroll(View.FOCUS_DOWN) }, 250)
            }
        }
    }

    private fun execScrollBack() {
        Handler().postDelayed({ getContentWrapperLayout().scrollTo(0,NumberHelper.nvl(scrollY,0))},250)
    }

    open fun execFullScrollUp() {
        getContentWrapperLayout().scrollTo(0, 0)
    }

    /**
     * @return true if all checks succeed
     */
    open fun execDoAdditionalCheck(): Boolean {
        return true
    }

    override fun onLayoutRendered() {
        super.onLayoutRendered()
        if (infoState == null) {
            execMorphInfoBar(InfoState.INITIALIZE)
        }
    }

    //************
    //* CREATION *
    //************
    enum class LineInputType {
        SINGLE_LINE_EDIT,
        MULTI_LINE_EDIT,
        LOOKUP, SINGLE_LINE_TEXT,
        MULTI_LINE_TEXT,
        SINGLE_LINE_CONTEXT_EDIT,
        MULTI_LINE_CONTEXT_EDIT,
        NUMBER_EDIT,
        NUMBER_SIGNED_EDIT,
        MULTI_TEXT,
        BUTTON
    }

    @Suppress("UNCHECKED_CAST")
    protected fun createLine(labelText: String, inputType: LineInputType, presetValue: String? = null, unchecked: Boolean, limit: Int, data: Any? = null, addExecutable: ((String?) -> Unit)? = null, removalExecutable: ((String?) -> Unit)? = null): View? {
        //Codes
        val singleSelect = SINGLE_SELECT == presetValue || SINGLE_SELECT_WITH_PRESET_POSITION.isContainedIn(presetValue)
        val readOnly = READ_ONLY == presetValue
        val noCompleteRemoval = COMPLETE_REMOVAL_DISABLED == presetValue || COMPLETE_REMOVAL_DISABLED_WITH_PRESET.isContainedIn(presetValue)
        val simpleLookup = SIMPLE_LOOKUP == presetValue
        var realPresetValue: String? = presetValue
        if (singleSelect && SINGLE_SELECT_WITH_PRESET_POSITION.length < presetValue!!.length){
            realPresetValue = presetValue.replace(SINGLE_SELECT_WITH_PRESET_POSITION,NOTHING)
        }else if (noCompleteRemoval && COMPLETE_REMOVAL_DISABLED_WITH_PRESET.length < presetValue!!.length ){
            realPresetValue = presetValue.replace(COMPLETE_REMOVAL_DISABLED_WITH_PRESET,NOTHING)
        }else if (singleSelect || readOnly || noCompleteRemoval|| simpleLookup){
            realPresetValue = null
        }
        if (CollectionHelper.oneOf(inputType, LineInputType.SINGLE_LINE_EDIT, LineInputType.MULTI_LINE_EDIT, LineInputType.NUMBER_EDIT, LineInputType.NUMBER_SIGNED_EDIT)) {
            val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            layoutParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            wrapper.weightSum = 2f
            wrapper.orientation = if (inputType == LineInputType.MULTI_LINE_EDIT) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            val label = SreTextView(this, wrapper, getString(R.string.label, labelText), BORDERLESS_DARK)
            label.setWeight(1f)
            label.setSize(WRAP_CONTENT, if (inputType == LineInputType.MULTI_LINE_EDIT) MATCH_PARENT else 0)
            label.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            val input = SreEditText(this, wrapper, null, getString(R.string.input, labelText))
            input.textAlignment = if (inputType == LineInputType.MULTI_LINE_EDIT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            input.textSize = textSize!!
            input.inputType = if (inputType == LineInputType.NUMBER_EDIT) InputType.TYPE_CLASS_NUMBER else if (inputType == LineInputType.NUMBER_SIGNED_EDIT) (InputType.TYPE_CLASS_NUMBER  or InputType.TYPE_NUMBER_FLAG_SIGNED )else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            if (inputType == LineInputType.NUMBER_EDIT){
                input.keyListener = DigitsKeyListener.getInstance("0123456789")
            }else if (CollectionHelper.oneOf(inputType, LineInputType.NUMBER_EDIT, LineInputType.NUMBER_SIGNED_EDIT)){
                input.addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val str = s.toString()
                        if (str.length > 1 && str.startsWith(ZERO_S)) {
                            val selection = if (input.selectionStart==1) 0 else str.length-1
                            s?.clear() //Avoid preceding 0 in Number-Fields
                            input.setText(str.substring(1))
                            input.setSelection(selection)
                        }
                    }

                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                        //NOP
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        //NOP
                    }
                })
            }
            input.setText(realPresetValue)
            input.setWeight(1f)
            input.setSize(MATCH_PARENT, if (inputType == LineInputType.MULTI_LINE_EDIT) MATCH_PARENT else 0)
            input.setSingleLine((inputType != LineInputType.MULTI_LINE_EDIT))
            if (isLandscapeOrientation()){
                //Present "Done" for every input to avoid jumping to unrelated inputs in landscape
                //Also prevents fullscreen cover-up in the input
                if (inputType == LineInputType.MULTI_LINE_EDIT){
                    input.imeOptions = EditorInfo.IME_ACTION_DONE
                }else{
                    input.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                }
            }
            if (limit > 0){
                input.filters = arrayOf(InputFilter.LengthFilter(limit))
            }
            wrapper.addView(label)
            wrapper.addView(input)
            if (unchecked){
                uncheckedMap[labelText] = input
            }else{
                inputMap[labelText] = input
            }
            return wrapper
        } else if (CollectionHelper.oneOf(inputType, LineInputType.SINGLE_LINE_CONTEXT_EDIT, LineInputType.MULTI_LINE_CONTEXT_EDIT)) {
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            childParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            wrapper.weightSum = 2f
            wrapper.orientation = if (inputType == LineInputType.MULTI_LINE_CONTEXT_EDIT) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            val label = SreTextView(this, wrapper, getString(R.string.label, labelText), BORDERLESS_DARK)
            label.setWeight(1f)
            label.setSize(WRAP_CONTENT, if (inputType == LineInputType.MULTI_LINE_EDIT) MATCH_PARENT else WRAP_CONTENT)
            label.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            val input = SreMultiAutoCompleteTextView(this, ArrayList())
            input.textAlignment = if (inputType == LineInputType.MULTI_LINE_CONTEXT_EDIT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            input.layoutParams = childParams
            input.textSize = textSize!!
            input.hint = StringHelper.applyFilters(labelText,applicationContext)
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            input.setText(realPresetValue)
            input.setSingleLine((inputType != LineInputType.MULTI_LINE_CONTEXT_EDIT))
            if (isLandscapeOrientation()){
                //Present "Done" for every input to avoid jumping to unrelated inputs in landscape
                //Also prevents fullscreen cover-up in the input
                if (inputType == LineInputType.MULTI_LINE_CONTEXT_EDIT){
                    input.imeOptions = EditorInfo.IME_ACTION_DONE
                }else{
                    input.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                }
            }
            wrapper.addView(label)
            wrapper.addView(input)
            inputMap[labelText] = input
            return wrapper
        } else if (CollectionHelper.oneOf(inputType, LineInputType.SINGLE_LINE_TEXT, LineInputType.MULTI_LINE_TEXT)) {
            val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            layoutParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            wrapper.orientation = if (inputType == LineInputType.MULTI_LINE_TEXT) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            val label = SreTextView(this, wrapper, getString(R.string.label, labelText), BORDERLESS_DARK)
            label.setSize(WRAP_CONTENT, if (inputType == LineInputType.MULTI_LINE_TEXT) MATCH_PARENT else WRAP_CONTENT)
            label.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            val scrollWrapper = ScrollView(this)
            val text = SreTextView(this, scrollWrapper, realPresetValue, MEDIUM)
            scrollWrapper.addView(text)
            text.textAlignment = if (inputType == LineInputType.MULTI_LINE_TEXT) View.TEXT_ALIGNMENT_TEXT_START else View.TEXT_ALIGNMENT_TEXT_END
            text.setSize(WRAP_CONTENT, if (inputType == LineInputType.MULTI_LINE_TEXT) MATCH_PARENT else WRAP_CONTENT)
            text.setSingleLine((inputType != LineInputType.MULTI_LINE_TEXT))
            wrapper.addView(label)
            wrapper.addView(scrollWrapper)
            inputMap[labelText] = text
            return wrapper
        } else if (CollectionHelper.oneOf(inputType, LineInputType.BUTTON)) {
            val layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            layoutParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            wrapper.orientation = VERTICAL
            val button = SreButton(this, wrapper, labelText,null,null, SreButton.ButtonStyle.ATTENTION)
            button.setSize(WRAP_CONTENT, MATCH_PARENT)
            if (data is Function0<*>){
                button.setExecutable { data.invoke()}
            }
            val scrollWrapper = ScrollView(this)
            wrapper.addView(button)
            wrapper.addView(scrollWrapper)
            return wrapper
        } else if (inputType == LineInputType.LOOKUP && data != null) {
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val childParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            childParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val wrapper = LinearLayout(this)
            val selectionCarrier = LinearLayout(this)
            val outerWrapper = LinearLayout(this)
            wrapper.layoutParams = layoutParams
            outerWrapper.layoutParams = layoutParams
            selectionCarrier.layoutParams = layoutParams
            wrapper.weightSum = 2f
            outerWrapper.weightSum = 2f
            wrapper.orientation = LinearLayout.HORIZONTAL
            outerWrapper.orientation = LinearLayout.VERTICAL
            selectionCarrier.orientation = LinearLayout.VERTICAL
            selectionCarrier.gravity = Gravity.CENTER
            val label = SreTextView(this, wrapper, getString(R.string.label, labelText), BORDERLESS_DARK)
            label.setSize(WRAP_CONTENT, MATCH_PARENT)
            label.setWeight(1f)
            label.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            val spinner = Spinner(applicationContext)
            val viewResource = resolveSpinnerLayoutStyle(applicationContext)
            val spinnerArrayAdapter = ArrayAdapter<String>(this, viewResource, data as Array<String>)
            spinnerArrayAdapter.setDropDownViewResource(viewResource)
            spinner.adapter = spinnerArrayAdapter
            spinner.dropDownVerticalOffset = textSize!!.toInt()
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val spinnerText = spinner.selectedItem as String
                    if (addExecutable != null) {
                        return addExecutable(null)
                    }
                    if (StringHelper.hasText(spinnerText)) {
                        for (t in 0 until selectionCarrier.childCount) {
                            if ((selectionCarrier.getChildAt(t) as TextView).text == spinnerText) {
                                spinner.setSelection(0)
                                return // Item already selected
                            }
                        }
                        if (!simpleLookup){
                            val selection = addSelection(spinnerText, selectionCarrier, labelText, spinner, null, singleSelect)
                            selection.data = position
                            if (singleSelect){
                                inputMap[labelText] = selection
                            }
                        }
                    }else if (singleSelect){
                        selectionCarrier.removeAllViews()
                        inputMap[labelText]?.text = NOTHING
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    //NOP
                }
            };
            spinner.setPadding(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            spinner.layoutParams = childParams
            wrapper.addView(label)
            wrapper.addView(spinner)
            outerWrapper.addView(wrapper)
            outerWrapper.addView(selectionCarrier)
            if (singleSelect){
                inputMap[labelText] = TextView(this)
                if (realPresetValue != null){
                    val pos = realPresetValue.toInt()
                    spinner.setSelection(pos)
                }
            }else {
                if (StringHelper.hasText(realPresetValue)) {
                    val split = realPresetValue!!.split(SEMI_COLON)
                    for (value in split) {
                        addSelection(value, selectionCarrier, labelText, spinner)
                    }
                }
            }
            tutorialOpen = SreTutorialLayoutDialog(this@AbstractManagementActivity,screenWidth,if (isLandscapeOrientation()) "info_lookup_landscape" else "info_lookup").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            return outerWrapper
        } else if (inputType == LineInputType.MULTI_TEXT){
            val wrapperParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            val topWrapperParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT,1f)
            wrapperParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
            val outerWrapper = LinearLayout(this)
            val wrapper = LinearLayout(this)
            val scrollView = ScrollView(this)
            val selectionCarrier = LinearLayout(this)
            outerWrapper.layoutParams = wrapperParams
            wrapper.layoutParams = wrapperParams
            selectionCarrier.layoutParams = wrapperParams
            outerWrapper.weightSum = 2f
            outerWrapper.orientation = LinearLayout.VERTICAL
            selectionCarrier.orientation = LinearLayout.VERTICAL
            selectionCarrier.gravity = Gravity.CENTER
            wrapper.weightSum = 2f
            wrapper.orientation = LinearLayout.VERTICAL
            val topWrapper = LinearLayout(this)
            topWrapper.layoutParams = topWrapperParams
            topWrapper.weightSum = 2f
            topWrapper.orientation = LinearLayout.HORIZONTAL
            val label = SreTextView(this, topWrapper, getString(R.string.label, labelText), BORDERLESS_DARK)
            val addButton = SreButton(this, topWrapper, getString(R.string.add),null,null)
            label.setWeight(1f)
            label.setSize(WRAP_CONTENT, MATCH_PARENT)
            label.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            addButton.setWeight(1f)
            addButton.setSize(WRAP_CONTENT, MATCH_PARENT)
            val objects = if (data is ArrayList<*>) data as ArrayList<Serializable> else ArrayList<Serializable>()
            val input = SreMultiAutoCompleteTextView(this, objects)
            input.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            input.textSize = textSize!!
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            input.setText(realPresetValue)
            input.setWeight(1f)
            input.setSize(MATCH_PARENT, MATCH_PARENT)
            input.setSingleLine(true)
            if (isLandscapeOrientation()){
                //Present "Done" for every input to avoid jumping to unrelated inputs in landscape
                //Also prevents fullscreen cover-up in the input
                input.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }
            addButton.setExecutable {
                val text = input.text.toString()
                if (StringHelper.hasText(text)){
                    addSelection(text, selectionCarrier, labelText, null, removalExecutable)
                }
                input.text = null
                addExecutable?.invoke(text)
            }
            if (readOnly) {
                addButton.visibility = View.GONE
                input.visibility = View.GONE
            }
            topWrapper.addView(label)
            topWrapper.addView(addButton)
            wrapper.addView(topWrapper)
            wrapper.addView(input)
            scrollView.addView(selectionCarrier)
            outerWrapper.addView(wrapper)
            outerWrapper.addView(scrollView)
            if (data != null){
                for (option in data as Array<String>){
                    val addSelection = addSelection(option, selectionCarrier, labelText,null, removalExecutable)
                    if (readOnly) {
                        addSelection.setExecutable { removalExecutable?.invoke(option) } //Override
                    }
                }
            }
            if (!readOnly && !noCompleteRemoval){
                addClearSelectionButton(getString(R.string.literal_remove_all), selectionCarrier, labelText)
            }
            uncheckedMap[labelText] = input
            tutorialOpen = SreTutorialLayoutDialog(this@AbstractManagementActivity,screenWidth,if (isLandscapeOrientation()) "info_multitext_landscape" else "info_multitext").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            return outerWrapper
        }
        return null
    }

    fun isLandscapeOrientation(): Boolean{
        val orientation = resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addSelection(text: String, selectionCarrier: LinearLayout, labelText: String, spinner: Spinner? = null, removalExecutable: ((String?) -> Unit)? = null, singleSelect: Boolean = false): SreButton {
        val textButton = SreButton(applicationContext, selectionCarrier, text, null,null, SreButton.ButtonStyle.DARK)
        val textParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        textParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
        textButton.layoutParams = textParams
        textButton.text = text
        textButton.textAlignment = View.TEXT_ALIGNMENT_CENTER
        textButton.setAllCaps(false)
        textButton.setExecutable {
            selectionCarrier.removeView(textButton)
            multiInputMap[labelText]?.remove(textButton)
            removalExecutable?.invoke(text)
            if (singleSelect){
                spinner?.setSelection(0)
            }
        }
        textButton.setLongClickOnly(true)
        if (singleSelect){
            selectionCarrier.removeAllViews()
        }
        selectionCarrier.addView(textButton,0)
        if (multiInputMap[labelText] == null) {
            val list = ArrayList<TextView>()
            list.add(textButton)
            multiInputMap[labelText] = list
        } else {
            multiInputMap[labelText]?.add(textButton)
        }
        if (!singleSelect){
            spinner?.setSelection(0)
        }
        return textButton
    }

    private fun addClearSelectionButton(text: String, selectionCarrier: LinearLayout, labelText: String) {
        val removalButton = SreButton(applicationContext, selectionCarrier, text, null,null, SreButton.ButtonStyle.ATTENTION)
        val textParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        textParams.setMargins(marginSmall!!, marginSmall!!, marginSmall!!, marginSmall!!)
        removalButton.layoutParams = textParams
        removalButton.text = text
        removalButton.textAlignment = View.TEXT_ALIGNMENT_CENTER
        removalButton.setAllCaps(false)
        removalButton.setExecutable {
            selectionCarrier.removeAllViews()
            multiInputMap[labelText]?.clear()
            selectionCarrier.addView(removalButton)
        }
        removalButton.setLongClickOnly(true)
        selectionCarrier.addView(removalButton)
    }
    //*******
    //* GUI *
    //*******
    protected fun changeLockState(): String {
        lockState = if (lockState == LockState.LOCKED) LockState.UNLOCKED else LockState.LOCKED
        return getLockIcon()
    }

    protected fun getLockIcon(): String {
        return when (lockState) {
            LockState.LOCKED -> resources.getString(R.string.icon_lock)
            LockState.UNLOCKED -> resources.getString(R.string.icon_lock_open)
        }
    }

    private fun collapseAndRefreshAllButtons() {
        for (v in 0 until getContentHolderLayout().childCount) {
            if (getContentHolderLayout().getChildAt(v) is SwipeButton) {
                val swipeButton = getContentHolderLayout().getChildAt(v) as SwipeButton
                if (swipeButton.state != SwipeButton.SwipeButtonState.MIDDLE) {
                    swipeButton.collapse()
                }
                when {
                    swipeButton.dataObject is Project -> swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Stakeholder::class, swipeButton.dataObject).size,
                            DatabaseHelper.getInstance(applicationContext).readBulk(Scenario::class, swipeButton.dataObject).size)
                    swipeButton.dataObject is Scenario -> swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(AbstractObject::class, swipeButton.dataObject).size, null)
                    swipeButton.dataObject is AbstractObject -> swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class, (swipeButton.dataObject as AbstractObject).id).size, null)
                }
            }
        }
    }

    protected fun cleanInfoHolder(titleText: String) {
        customizeToolbarText(resources.getString(R.string.icon_check), null, null, null, resources.getString(R.string.icon_cross))
        getInfoTitle().text = titleText
        getInfoContent().visibility = View.GONE
        removeExcept(getInfoContentWrap(), getInfoContent())
        inputMap.clear()
        uncheckedMap.clear()
        multiInputMap.clear()
        getInfoContentWrap().orientation = LinearLayout.VERTICAL
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        getInfoContentWrap().layoutParams = layoutParams
    }

    protected fun showDeletionConfirmation(objectName: String?) {
        if (objectName == null) {
            return
        }
        if (!infoShowing){
            textPrior = getInfoTitle().text
            textColorPrior = getInfoTitle().currentTextColor
            infoShowing = true
        }
        getInfoTitle().text = resources.getString(R.string.x_deleted, objectName)
        getInfoTitle().setTextColor(ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn))
        resetInfo()
    }

    protected fun showInfoText(infoText: String, color: Int = R.color.sreTutorial) {
        if (!infoShowing){
            textPrior = getInfoTitle().text
            textColorPrior = getInfoTitle().currentTextColor
            infoShowing = true
        }
        getInfoTitle().text = infoText
        getInfoTitle().setTextColor(getColorWithStyle(applicationContext,color))
        resetInfo(2000L+(50*infoText.length))
    }

    private var textPrior: CharSequence? = null
    private var textColorPrior: Int? = null
    private var infoShowing = false
    private var handlerId = 0L
    private fun resetInfo(time: Long = 1000) {
        val localHandlerId = Random().nextLong()
        handlerId = localHandlerId
        Handler().postDelayed({
            if (localHandlerId == handlerId) {
                getInfoTitle().text = textPrior
                getInfoTitle().setTextColor(NumberHelper.nvl(textColorPrior,getColorWithStyle(applicationContext,R.color.sreWhite)))
                infoShowing = false
            }
        }, time)
    }

    fun isInputOpen(): Boolean{
        return (isInEditMode() || isInAddMode())
    }
    abstract fun isInEditMode(): Boolean
    abstract fun isInAddMode(): Boolean
    abstract fun isInViewMode(): Boolean
    abstract fun resetEditMode()
    abstract fun createEntity()
    abstract fun getConfiguredInfoString(): Int
    open fun isSpacingEnabled(): Boolean {
        return false
    }
    open fun isCanceling(): Boolean {
        return false
    }

    open fun getContentWrapperLayout(): ViewGroup {
        return scroll_holder_scroll
    }

    open fun getContentHolderLayout(): ViewGroup {
        return scroll_holder_linear_layout_holder
    }

    open fun getInfoWrapper(): LinearLayout {
        return scroll_holder_layout_info
    }

    open fun getInfoTitle(): TextView {
        return scroll_holder_text_info_title
    }

    open fun getInfoContentWrap(): LinearLayout {
        return scroll_holder_text_info_content_wrap
    }

    open fun getInfoContent(): TextView {
        return scroll_holder_text_info_content
    }

    open fun resetToolbar() {
        customizeToolbarText(resources.getText(R.string.icon_back).toString(), null, getLockIcon(), resources.getText(R.string.icon_glossary).toString(), null)
    }

    //*************
    //* EXECUTION *
    //************
    enum class InfoState {
        MINIMIZED, NORMAL, MAXIMIZED, INITIALIZE
    }

    private var infoState: InfoState? = null

    protected fun execMorphInfoBar(state: InfoState? = null, maxLines: Int = 10): CharSequence {
        if (state != null) {
            infoState = state
        } else {
            when (infoState) {
                InfoState.MINIMIZED -> infoState = InfoState.NORMAL
                InfoState.NORMAL -> infoState = InfoState.MAXIMIZED
                InfoState.MAXIMIZED -> infoState = InfoState.MINIMIZED
                else -> {
                } //NOP
            }
        }
        return execMorphInfoBarInternal(maxLines)
    }

    protected var contentDefaultMaxLines = 2
    private fun execMorphInfoBarInternal(maxLines: Int): CharSequence {
        when (infoState) {
            InfoState.INITIALIZE -> {
                getContentWrapperLayout().layoutParams = createLayoutParams(1f)
                getInfoWrapper().layoutParams = createLayoutParams(9f)
                createLayoutParams(0f, getInfoTitle())
                getInfoContentWrap().layoutParams = createLayoutParams(1f)
                infoState = InfoState.MINIMIZED
                return resources.getText(R.string.icon_win_min)
            }
            InfoState.MINIMIZED -> {
                WeightAnimator(getInfoWrapper(), 9f, 250).play()
                WeightAnimator(getContentWrapperLayout(), 1f, 250).play()
                createLayoutParams(0f, getInfoTitle())
                getInfoContentWrap().layoutParams = createLayoutParams(1f)
                execMinimizeKeyboard()
                return resources.getText(R.string.icon_win_min)
            }
            InfoState.NORMAL -> {
                WeightAnimator(getContentWrapperLayout(), 3f, 250).play()
                WeightAnimator(getInfoWrapper(), 7f, 250).play()
                createLayoutParams(2f, getInfoTitle(), 1)
                getInfoContentWrap().layoutParams = createLayoutParams(1f)
                 getInfoContent().maxLines = contentDefaultMaxLines
                return resources.getText(R.string.icon_win_norm)
            }
            InfoState.MAXIMIZED -> {
                scrollY = getContentWrapperLayout().scrollY
                WeightAnimator(getContentWrapperLayout(), 10f, 250).play()
                WeightAnimator(getInfoWrapper(), 0f, 250).play()
                createLayoutParams(2.7f, getInfoTitle(), 1)
                getInfoContentWrap().layoutParams = createLayoutParams(0.3f)
                getInfoContent().maxLines = maxLines
                return resources.getText(R.string.icon_win_max)
            }
        }
        return resources.getText(R.string.icon_null)
    }

    fun getTextsFromLookupChoice(id: String): ArrayList<String> {
        val list = ArrayList<String>()
        if (!StringHelper.hasText(id)) {
            return list
        }
        val multi = multiInputMap[id]
        if (multi == null || multi.isEmpty()) {
            return list
        }
        for (text in multi) {
            list.add(text.text.toString())
        }
        return list
    }
}