package uzh.scenere.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_attributes.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.datamodel.AbstractObject
import uzh.scenere.datamodel.Attribute
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getColorWithStyle
import uzh.scenere.helpers.getStringValue
import uzh.scenere.views.SwipeButton

class AttributesActivity : AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return attributes_root
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_explain_attributes
    }
    override fun getConfiguredLayout(): Int {
        return R.layout.activity_attributes
    }

    enum class AttributeMode {
        VIEW, EDIT_CREATE
    }
    private var attributesMode: AttributeMode = AttributeMode.VIEW
    override fun isInViewMode(): Boolean {
        return attributesMode == AttributeMode.VIEW
    }

    override fun isInEditMode(): Boolean {
        return attributesMode == AttributeMode.EDIT_CREATE
    }

    override fun isInAddMode(): Boolean {
        return attributesMode == AttributeMode.EDIT_CREATE
    }

    override fun resetEditMode() {
        activeAttribute = null
        attributesMode = AttributeMode.VIEW
    }

    private lateinit var inputLabelKey: String
    private lateinit var inputLabelValue: String
    private var activeObject: AbstractObject? = null
    private var activeAttribute: Attribute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputLabelKey = getString(R.string.attribute_name)
        inputLabelValue = getString(R.string.attribute_description)
        activeObject = intent.getSerializableExtra(Constants.BUNDLE_OBJECT) as AbstractObject
        creationButton =
                SwipeButton(this, getString(R.string.attribute_create))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setButtonStates(false, true, false, false)
                        .setButtonIcons(R.string.icon_null, R.string.icon_edit, null, null, R.string.icon_attributes)
                        .setFirstPosition()
                        .updateViews(true)
        creationButton!!.setExecutable(generateCreationExecutable(creationButton!!))
        getContentHolderLayout().addView(creationButton)
        createTitle("", getContentHolderLayout())
        for (attribute in DatabaseHelper.getInstance(applicationContext).readBulk(Attribute::class, activeObject!!.id)) {
            addAttributeToList(attribute)
        }
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(R.string.icon_explain_attributes), fontAwesome)
        resetToolbar()
    }

    private fun addAttributeToList(attribute: Attribute) {
        val swipeButton = SwipeButton(this, attribute.key)
                .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                .setButtonIcons(R.string.icon_delete, R.string.icon_edit,null,null, null)
                .setButtonStates(lockState == LockState.UNLOCKED, true, false, false)
                .updateViews(true)
        swipeButton.dataObject = attribute
        swipeButton.setExecutable(generateAttributeExecutable(swipeButton, attribute))
        getContentHolderLayout().addView(swipeButton)
    }

    private fun generateCreationExecutable(button: SwipeButton, attribute: Attribute? = null): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                activeButton = button
                openInput(AttributeMode.EDIT_CREATE)
            }
        }
    }

    private fun generateAttributeExecutable(button: SwipeButton, attribute: Attribute? = null): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execLeft() {
                if (attribute != null) {
                    removeAttribute(attribute, true)
                    showDeletionConfirmation(attribute.key)
                }
            }
            override fun execRight() {
                activeButton = button
                openInput(AttributeMode.EDIT_CREATE, attribute)
            }
            override fun execReset() {
                resetEditMode()
            }
        }
    }

    override fun createEntity() {
        val key = inputMap[inputLabelKey]!!.getStringValue()
        val value = inputMap[inputLabelValue]!!.getStringValue()
        val attributeBuilder = Attribute.AttributeBuilder(activeObject!!.id, key, value)
        if (activeAttribute != null) {
            removeAttribute(activeAttribute!!)
            attributeBuilder.copyId(activeAttribute!!)
        }
        val attribute = attributeBuilder.build()
        DatabaseHelper.getInstance(applicationContext).write(attribute.id, attribute)
        addAttributeToList(attribute)
    }

    private fun openInput(attributesMode: AttributeMode, attribute: Attribute? = null) {
        activeAttribute = attribute
        this.attributesMode = attributesMode
        when (attributesMode) {
            AttributeMode.VIEW -> {}//NOP
            AttributeMode.EDIT_CREATE -> {
                cleanInfoHolder(if (activeAttribute == null) getString(R.string.attributes_create) else getString(R.string.attributes_edit))
                getInfoContentWrap().addView(createLine(inputLabelKey, LineInputType.SINGLE_LINE_EDIT, attribute?.key, false, -1))
                getInfoContentWrap().addView(createLine(inputLabelValue, LineInputType.MULTI_LINE_EDIT, attribute?.value, false, -1))
            }
        }
        execMorphInfoBar(InfoState.MAXIMIZED)
    }

    private fun removeAttribute(attribute: Attribute, dbRemoval: Boolean = false) {
        for (viewPointer in 0 until getContentHolderLayout().childCount) {
            if (getContentHolderLayout().getChildAt(viewPointer) is SwipeButton &&
                    (getContentHolderLayout().getChildAt(viewPointer) as SwipeButton).dataObject == attribute) {
                getContentHolderLayout().removeViewAt(viewPointer)
                if (dbRemoval){
                    DatabaseHelper.getInstance(applicationContext).delete(attribute.id, Attribute::class)
                }
                return
            }
        }
    }

    override fun onToolbarCenterRightClicked() {
        if (!isInputOpen()) {
            val intent = Intent(this, GlossaryActivity::class.java)
            intent.putExtra(Constants.BUNDLE_GLOSSARY_TOPIC, "Attribute")
            intent.putExtra(Constants.BUNDLE_GLOSSARY_ADDITIONAL_TOPICS, arrayOf("Resource"))
            startActivity(intent)
        }
    }
}