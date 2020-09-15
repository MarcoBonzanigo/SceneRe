package uzh.scenere.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import kotlinx.android.synthetic.main.activity_objects.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.BUNDLE_SCENARIO
import uzh.scenere.const.Constants.Companion.HALF_SEC_MS
import uzh.scenere.const.Constants.Companion.SIMPLE_LOOKUP
import uzh.scenere.datamodel.*
import uzh.scenere.helpers.*
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButton.SwipeButtonExecution


class ObjectsActivity : AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return objects_root
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_objects
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_objects
    }

    enum class ObjectMode{
        VIEW, EDIT, CREATE, ATTRIBUTES
    }
    private var objectsMode: ObjectMode = ObjectMode.VIEW
    override fun isInViewMode(): Boolean {
        return objectsMode == ObjectMode.VIEW
    }

    override fun isInEditMode(): Boolean {
        return objectsMode == ObjectMode.EDIT
    }

    override fun isInAddMode(): Boolean {
        return objectsMode == ObjectMode.CREATE
    }

    override fun resetEditMode() {
        activeObject = null
        isResourceSpinner = null
        objectsMode = ObjectMode.VIEW
    }

    private lateinit var inputLabelName: String
    private lateinit var inputLabelDescription: String
    private lateinit var inputLabelResource: String
    private var activeScenario: Scenario? = null
    private var activeObject: AbstractObject? = null
    private var isResourceSpinner: View? = null
    private var inputInvalid = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputLabelName = getString(R.string.object_name)
        inputLabelDescription = getString(R.string.object_description)
        inputLabelResource = getString(R.string.object_resource)
        
        activeScenario = intent.getSerializableExtra(BUNDLE_SCENARIO) as Scenario
        creationButton =
                SwipeButton(this,getString(R.string.object_create))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel),ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setButtonStates(false,true,false,false)
                        .setButtonIcons(R.string.icon_null,R.string.icon_edit,null,null,R.string.icon_object)
                        .setFirstPosition()
                        .updateViews(true )
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        getContentHolderLayout().addView(creationButton)
        createTitle("",getContentHolderLayout())
        loadData()
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_objects),fontAwesome)
        resetToolbar()
    }

    private fun loadData() {
        removeExcept(getContentHolderLayout(),creationButton)
        for (obj in DatabaseHelper.getInstance(applicationContext).readBulk(AbstractObject::class, activeScenario)) {
            addObjectToList(obj)
        }
    }

    private fun addObjectToList(obj: AbstractObject) {
        val swipeButton = SwipeButton(this, obj.name)
                .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                .setButtonMode(SwipeButton.SwipeButtonMode.QUADRUPLE)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit, R.string.icon_attributes, R.string.icon_null, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, true, false)
                .updateViews(true)
        swipeButton.dataObject = obj
        swipeButton.setCounter(DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class,obj.id).size,null)
        swipeButton.setExecutable(generateObjectExecutable(swipeButton, obj))
        getContentHolderLayout().addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, obj: AbstractObject? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execRight() {
                activeButton = button
                openInput(ObjectMode.CREATE)
            }
        }
    }

    private fun generateObjectExecutable(button: SwipeButton, obj: AbstractObject? = null): SwipeButtonExecution {
        return object: SwipeButtonExecution{
            override fun execLeft() {
                if (obj!=null){
                    removeObject(obj,true)
                    showDeletionConfirmation(obj.name)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(ObjectMode.EDIT,obj)
            }
            override fun execUp() {
                activeButton = button
                openInput(ObjectMode.ATTRIBUTES,obj)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
        val name = inputMap[inputLabelName]!!.getStringValue()
        val introduction = inputMap[inputLabelDescription]!!.getStringValue()
        var isResource = false
        if (isResourceSpinner != null){
            val spinner = searchForLayout(isResourceSpinner!!, Spinner::class)
            isResource = spinner?.selectedItem.toString()=="True"
        }
        val objectBuilder: AbstractObject.AbstractObjectBuilder?
        if (isResource){
            objectBuilder = Resource.ResourceBuilder(activeScenario!!, name, introduction).configure(
            inputMap[min]!!.getStringValue().toInt(),
            inputMap[max]!!.getStringValue().toInt(),
            inputMap[init]!!.getStringValue().toInt())
        }else{
            objectBuilder = ContextObject.ContextObjectBuilder(activeScenario!!, name, introduction)
        }
        if (activeObject != null){
            removeObject(activeObject!!)
            objectBuilder.copyId(activeObject!!)
        }
        val obj = objectBuilder.build()
        if (obj is Resource && obj.min >= obj.max){
            reOpenInput(obj,R.string.objects_min_max_alert)
        }else if (obj is Resource && obj.init > obj.max){
            reOpenInput(obj,R.string.objects_init_max_alert)
        }else if (obj is Resource && obj.init < obj.min){
            reOpenInput(obj,R.string.objects_init_min_alert)
        }else{
            DatabaseHelper.getInstance(applicationContext).write(obj.id,obj)
            addObjectToList(obj)
        }
    }

    private fun reOpenInput(obj: AbstractObject, notification: Int) {
        inputInvalid = true
        Handler().postDelayed({
            openInput(ObjectMode.EDIT, obj)
            notify(getString(R.string.objects_min_max_alert_title), getString(notification))
        }, HALF_SEC_MS)
    }

    private fun openInput(objectsMode: ObjectMode, obj: AbstractObject? = null) {
        activeObject = obj
        this.objectsMode = objectsMode
        when(objectsMode){
            ObjectMode.VIEW -> {}//NOP
            ObjectMode.EDIT, ObjectMode.CREATE -> {
                cleanInfoHolder(if (activeObject==null) getString(R.string.objects_create) else getString(R.string.objects_edit))
                getInfoContentWrap().addView(createLine(inputLabelName, LineInputType.SINGLE_LINE_EDIT, obj?.name, false, -1))
                isResourceSpinner = createLine(inputLabelResource, LineInputType.LOOKUP, SIMPLE_LOOKUP, false, -1, if (ObjectHelper.nvl(obj?.isResource,false)) arrayOf("True", "False") else arrayOf("False", "True"), { execResourceStateChanged() })
                getInfoContentWrap().addView(isResourceSpinner)
                getInfoContentWrap().addView(createLine(inputLabelDescription, LineInputType.MULTI_LINE_EDIT, obj?.description, false, -1))
            }
            ObjectMode.ATTRIBUTES -> {
                val intent = Intent(this, AttributesActivity::class.java)
                intent.putExtra(Constants.BUNDLE_OBJECT, activeObject)
                startActivity(intent)
                return
            }
        }
        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private val min = "Minimum"
    private val max = "Maximum"
    private val init = "Initial"
    private var minResourceLayout: View? = null
    private var maxResourceLayout: View? = null
    private var initResourceLayout: View? = null
    private fun execResourceStateChanged(){
        var isResource = false
        if (isResourceSpinner != null){
            val spinner = searchForLayout(isResourceSpinner!!, Spinner::class)
            isResource = spinner?.selectedItem.toString()=="True"
        }
        if (isResource){
            val resource: Resource? = if (activeObject is Resource) activeObject as Resource else null
            minResourceLayout = createLine(min, LineInputType.NUMBER_SIGNED_EDIT, if (resource?.min == null) null else resource.min.toString(), false, 9)
            maxResourceLayout = createLine(max, LineInputType.NUMBER_SIGNED_EDIT, if (resource?.max == null) null else resource.max.toString(), false, 9)
            initResourceLayout = createLine(init, LineInputType.NUMBER_SIGNED_EDIT, if (resource?.init == null) null else resource.init.toString(), false, 9)
            getInfoContentWrap().addView(minResourceLayout,getInfoContentWrap().childCount-1)
            getInfoContentWrap().addView(maxResourceLayout,getInfoContentWrap().childCount-1)
            getInfoContentWrap().addView(initResourceLayout,getInfoContentWrap().childCount-1)
        }else{
            getInfoContentWrap().removeView(minResourceLayout)
            getInfoContentWrap().removeView(maxResourceLayout)
            getInfoContentWrap().removeView(initResourceLayout)
            minResourceLayout = null
            maxResourceLayout = null
            initResourceLayout = null
            inputMap.remove(min)
            inputMap.remove(max)
            inputMap.remove(init)
        }
    }

    private fun removeObject(obj: AbstractObject, dbRemoval: Boolean = false) {
        for (viewPointer in 0 until getContentHolderLayout().childCount){
            if (getContentHolderLayout().getChildAt(viewPointer) is SwipeButton &&
                    (getContentHolderLayout().getChildAt(viewPointer) as SwipeButton).dataObject == obj){
                getContentHolderLayout().removeViewAt(viewPointer)
                if (dbRemoval) {
                    DatabaseHelper.getInstance(applicationContext).delete(obj.id, AbstractObject::class)
                }
                return
            }
        }
    }

    override fun execDoAdditionalCheck(): Boolean {
        val nameField = inputMap[inputLabelName] ?: return true
        if (StringHelper.hasText(nameField.getStringValue())) {
            if (objectsMode == ObjectMode.EDIT){
                return true
            }
            for (v in 0 until getContentHolderLayout().childCount) {
                if ((getContentHolderLayout().getChildAt(v) is SwipeButton) && (((getContentHolderLayout().getChildAt(v)) as SwipeButton).getText() == nameField.getStringValue())) {
                    notify(getString(R.string.objects_similar_alert))
                    return false
                }
            }
        }
        return true
    }

    override fun onToolbarCenterRightClicked() {
        if (!isInputOpen()) {
            val intent = Intent(this, GlossaryActivity::class.java)
            intent.putExtra(Constants.BUNDLE_GLOSSARY_TOPIC, "Object")
            intent.putExtra(Constants.BUNDLE_GLOSSARY_ADDITIONAL_TOPICS, arrayOf("Resource","Attribute"))
            startActivity(intent)
        }
    }

    override fun onToolbarRightClicked() {
        if (isInputOpen() && inputInvalid){
            super.onToolbarRightClicked()
            loadData()
            inputInvalid = false
        }else{
            super.onToolbarRightClicked()
        }
    }
}