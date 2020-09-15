package uzh.scenere.activities

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ScrollView
import kotlinx.android.synthetic.main.activity_glossary.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.helpers.StringHelper
import uzh.scenere.helpers.getColorWithStyle
import uzh.scenere.views.SreScrollView
import uzh.scenere.views.SreTextView
import uzh.scenere.views.SwipeButton
import uzh.scenere.views.SwipeButtonSortingLayout

class GlossaryActivity: AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return glossary_root
    }

    override fun isInEditMode(): Boolean {
        return inputOpen
    }

    override fun isInAddMode(): Boolean {
        return inputOpen
    }

    override fun isInViewMode(): Boolean {
        return true
    }

    override fun resetEditMode() {
        //NOP
    }

    override fun createEntity() {
        //NOP
    }

    override fun getConfiguredInfoString(): Int {
        return R.string.icon_null
    }

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_glossary
    }

    override fun resetToolbar() {
        customizeToolbarId(R.string.icon_back,null,null,null,null)
    }

    private val buttonMap = HashMap<String,SwipeButton>()
    private var inputOpen = false
    private var scrollContainer: ScrollView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val topic = intent.getStringExtra(Constants.BUNDLE_GLOSSARY_TOPIC)
        val additional = intent.getStringArrayExtra(Constants.BUNDLE_GLOSSARY_ADDITIONAL_TOPICS)
        buttonMap[getString(R.string.literal_project)] = createGlossaryButton(getString(R.string.literal_project),R.string.icon_project, R.string.glossary_project)
        buttonMap[getString(R.string.literal_stakeholder)] = createGlossaryButton(getString(R.string.literal_stakeholder),R.string.icon_stakeholder, R.string.glossary_stakeholder)
        buttonMap[getString(R.string.literal_scenario)] = createGlossaryButton(getString(R.string.literal_scenario),R.string.icon_scenario, R.string.glossary_scenario)
        buttonMap[getString(R.string.literal_object)] = createGlossaryButton(getString(R.string.literal_object),R.string.icon_object, R.string.glossary_object)
        buttonMap[getString(R.string.literal_attribute)] = createGlossaryButton(getString(R.string.literal_attribute),R.string.icon_attributes, R.string.glossary_attribute)
        buttonMap[getString(R.string.literal_resource)] = createGlossaryButton(getString(R.string.literal_resource),R.string.icon_resource, R.string.glossary_resource)
        buttonMap[getString(R.string.literal_walkthrough)] = createGlossaryButton(getString(R.string.literal_walkthrough),R.string.icon_walkthrough, R.string.glossary_walkthrough)
        buttonMap[getString(R.string.literal_step)] = createGlossaryButton(getString(R.string.literal_step),R.string.icon_step, R.string.glossary_step)
        buttonMap[getString(R.string.literal_trigger)] = createGlossaryButton(getString(R.string.literal_trigger),R.string.icon_trigger, R.string.glossary_trigger)
        buttonMap[getString(R.string.literal_editor)] = createGlossaryButton(getString(R.string.literal_editor),R.string.icon_path_editor, R.string.glossary_editor)
        buttonMap[getString(R.string.literal_what_if)] = createGlossaryButton(getString(R.string.literal_what_if),R.string.icon_what_if, R.string.glossary_what_if)

        var button: SwipeButton? = null
        for (entry in buttonMap.entries){
            if (entry.key == topic){
                button = entry.value
                entry.value.setIndividualButtonColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn),ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn),ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn),ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn),ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn)).updateViews(false)
            }
            if (additional != null){
                for (add in additional){
                    if (entry.key == add) {
                        entry.value.setIndividualButtonColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryAttention), ContextCompat.getColor(applicationContext,R.color.srePrimaryAttention), ContextCompat.getColor(applicationContext,R.color.srePrimaryAttention), ContextCompat.getColor(applicationContext,R.color.srePrimaryAttention), ContextCompat.getColor(applicationContext,R.color.srePrimaryAttention)).updateViews(false)
                    }
                }
            }
            getContentHolderLayout().addView(entry.value)
        }
        if (button != null && getContentHolderLayout() is SwipeButtonSortingLayout){
            (getContentHolderLayout() as SwipeButtonSortingLayout).scrollTo(button)
        }

        scrollContainer = SreScrollView(applicationContext,getInfoContent().parent as ViewGroup)
        (getInfoContent().parent as ViewGroup).addView(scrollContainer)
        getInfoContent().visibility = GONE
        resetToolbar()
    }

    private fun createGlossaryButton(label: String, icon: Int, glossaryText: Int): SwipeButton {
        return SwipeButton(this, label)
                .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                .setButtonStates(false, true, false, false)
                .setButtonIcons(R.string.icon_null, R.string.icon_glossary_entry, null, null, icon)
                .setExecutable(object: SwipeButton.SwipeButtonExecution{
                    override fun execRight() {
                        inputOpen = true
                        getInfoTitle().text = label
                        val info = SreTextView(applicationContext,scrollContainer,NOTHING)
                        info.text = StringHelper.fromHtml(resources.getString(glossaryText))
                        scrollContainer?.removeAllViews()
                        scrollContainer?.addView(info)
                        execMorphInfoBar(InfoState.MAXIMIZED,100)
                        customizeToolbarId(R.string.icon_null,null,null,null,R.string.icon_cross)
                    }
                })
                .setAutoCollapse(true)
                .updateViews(true)
    }

    override fun onToolbarCenterClicked() {
        //NOP
    }

    override fun onToolbarRightClicked() {
        getInfoTitle().text = null
        getInfoContent().text = null
        execMorphInfoBar(InfoState.MINIMIZED)
        resetToolbar()
        inputOpen = false
        scrollContainer?.removeAllViews()
    }

    override fun getIsFirstScrollUp(): Boolean {
        return false
    }

    override fun onToolbarLeftClicked() {
        if (!inputOpen){
            super.onToolbarLeftClicked()
        }
    }
}