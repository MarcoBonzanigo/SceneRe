package uzh.scenere.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_cockpit.*
import kotlinx.android.synthetic.main.sre_toolbar.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.IS_ADMINISTRATOR
import uzh.scenere.const.Constants.Companion.IS_RELOAD
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.PERMISSION_REQUEST_ALL
import uzh.scenere.const.Constants.Companion.PERMISSION_REQUEST_GPS
import uzh.scenere.const.Constants.Companion.PNG_FILE
import uzh.scenere.const.Constants.Companion.STYLE
import uzh.scenere.const.Constants.Companion.TUTORIAL_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.WHAT_IF_DATA
import uzh.scenere.const.Constants.Companion.WHAT_IF_MODE
import uzh.scenere.helpers.*
import uzh.scenere.sensors.SensorHelper
import uzh.scenere.views.SreTutorialLayoutDialog
import uzh.scenere.views.SwipeButton


class CockpitActivity : AbstractManagementActivity() {

    override fun getConfiguredRootLayout(): ViewGroup? {
        return cockpit_root
    }

    override fun isInEditMode(): Boolean {
        return false
    }

    override fun isInAddMode(): Boolean {
        return false
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
        return R.string.icon_explain_cockpit
    }

    enum class CockpitMode(var id: Int, var label: String, var description: Int) {
        PERMISSIONS(0, "Missing Permissions", R.string.cockpit_info_permissions),
        COMMUNICATIONS(1, "Communication Systems", R.string.cockpit_info_communications),
        SENSORS(2, "Available Sensors", R.string.cockpit_info_sensors),
        FUNCTIONS(3,"Administrator Functions",R.string.cockpit_info_admin);

        fun next(): CockpitMode {
            return get((id + 1))
        }

        fun previous(): CockpitMode {
            return get((id - 1))
        }

        private fun get(id: Int): CockpitMode {
            for (m in CockpitMode.values()) {
                if (m.id == id)
                    return m
            }
            return if (id < 0) get(CockpitMode.values().size - 1) else get(0)
        }

        fun getDescription(context: Context): String {
            return context.resources.getString(description)
        }
    }

    private var mode: CockpitMode = CockpitMode.PERMISSIONS

    override fun getConfiguredLayout(): Int {
        return R.layout.activity_cockpit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(IS_RELOAD,false)){
            mode = CockpitMode.FUNCTIONS
            DatabaseHelper.getInstance(applicationContext).write(IS_RELOAD,true,DatabaseHelper.DataMode.PREFERENCES)
        }
        creationButton = SwipeButton(this, getString(R.string.cockpit_mode))
                .setColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                .setButtonIcons(R.string.icon_backward, R.string.icon_forward, R.string.icon_null, R.string.icon_null, null)
                .setButtonStates(true,true, false, false)
                .adaptMasterLayoutParams(true)
                .setFirstPosition()
                .setExecutable(object: SwipeButton.SwipeButtonExecution{
                    override fun execLeft() {
                        onLeftClicked()
                    }
                    override fun execRight() {
                        onRightClicked()
                    }
                })
                .setAutoCollapse(true)
                .updateViews(true)
        getContentHolderLayout().addView(creationButton)
        customizeToolbarId(R.string.icon_back, R.string.icon_null, R.string.icon_win_min, R.string.icon_null, null)
        getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
        recreateViews()
        if (PermissionHelper.getRequiredPermissions(this).isEmpty()){
            tutorialOpen = SreTutorialLayoutDialog(this@CockpitActivity,screenWidth,"info_navigate","info_toolbar").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        }else{
            tutorialOpen = SreTutorialLayoutDialog(this@CockpitActivity,screenWidth,"info_navigate", "info_toolbar","info_permissions").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ALL && permissions.isNotEmpty()) {
            var granted = true
            if (grantResults.isNotEmpty()) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        granted = false
                    }
                }
            } else {
                granted = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_REQUEST_GPS && resultCode == Activity.RESULT_OK){
            CommunicationHelper.registerGpsListener(this@CockpitActivity)
        }
        recreateViews()
    }

    override fun onResume() {
        super.onResume()
        // Persist Username on granting Permission
        DatabaseHelper.getInstance(applicationContext).readAndMigrate(Constants.USER_NAME, String::class, NOTHING, false)
        DatabaseHelper.getInstance(applicationContext).readAndMigrate(Constants.USER_ID, String::class, NOTHING, false)
        recreateViews()
    }

    fun onRightClicked() {
        SensorHelper.getInstance(this).unregisterTextGraphListener()
        mode = mode.next()
        getInfoContent().text = mode.getDescription(applicationContext)
        recreateViews()
    }

    fun onLeftClicked() {
        SensorHelper.getInstance(this).unregisterTextGraphListener()
        mode = mode.previous()
        getInfoContent().text = mode.getDescription(applicationContext)
        recreateViews()
    }

    override fun onToolbarCenterClicked() {
        toolbar_action_center.text = execMorphInfoBar(null)
    }

    private fun recreateViews() {
        getInfoContent().text = mode.getDescription(applicationContext)
        removeExcept(getContentHolderLayout(),creationButton)
        getContentWrapperLayout().scrollTo(0,0)
        creationButton?.setText(mode.label)
        when (this.mode) {
            CockpitMode.PERMISSIONS -> {
                for (permission in PermissionHelper.getRequiredPermissions(this)) {
                    val swipeButton = SwipeButton(applicationContext, permission.label)
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setButtonIcons(R.string.icon_info,R.string.icon_check, null, null, null)
                            .setButtonStates(true, true, false, false)
                            .setAutoCollapse(true)
                            .updateViews(true)
                    swipeButton.dataObject = permission
                    swipeButton.outputObject = getInfoContent()
                    swipeButton.setExecutable(generatePermissionExecutable(permission, swipeButton))
                    getContentHolderLayout().addView(swipeButton)
                }
            }
            CockpitMode.COMMUNICATIONS -> {
                for (communication in CommunicationHelper.getCommunications()) {
                    if (communication != CommunicationHelper.Companion.Communications.GPS){
                        val granted = CommunicationHelper.check(this, communication)
                        val swipeButton = SwipeButton(this, communication.label)
                                .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                                .setIndividualButtonColors(if (granted) getColorWithStyle(applicationContext,R.color.srePrimaryPastel) else ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn), if (granted) getColorWithStyle(applicationContext,R.color.srePrimarySafe) else getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                                .setButtonIcons(R.string.icon_cross, R.string.icon_check, null, null, null)
                                .setButtonStates(true, true, false, false)
                                .setAutoCollapse(true)
                                .updateViews(true)
                        swipeButton.dataObject = communication
                        swipeButton.outputObject = getInfoContent()
                        swipeButton.setExecutable(generateCommunicationExecutable(communication, swipeButton))
                        getContentHolderLayout().addView(swipeButton)
                    }
                }
                tutorialOpen = SreTutorialLayoutDialog(this@CockpitActivity,screenWidth,"info_communications").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            }
            CockpitMode.SENSORS -> {
                for (sensor in SensorHelper.getInstance(this).getSensorArray()) {
                    val swipeButton = SwipeButton(this, SensorHelper.getInstance(this).getTypeName(sensor.name))
                            .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                            .setIndividualButtonColors(getColorWithStyle(applicationContext,R.color.srePrimaryPastel), getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                            .setButtonIcons(R.string.icon_eye_closed, R.string.icon_eye, null, null, null)
                            .setButtonStates(true, true, false, false)
                            .setAutoCollapse(true)
                            .updateViews(true)
                    swipeButton.dataObject = sensor
                    swipeButton.outputObject = getInfoContent()
                    swipeButton.setExecutable(generateSensorExecutable(sensor, swipeButton))
                    getContentHolderLayout().addView(swipeButton)
                }
                tutorialOpen = SreTutorialLayoutDialog(this@CockpitActivity,screenWidth,"info_sensors").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            }
            CockpitMode.FUNCTIONS -> {
                val resetTutorial = SwipeButton(this, getString(R.string.cockpit_tutorial_reset))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setExecutable(object : SwipeButton.SwipeButtonExecution{
                            override fun execRight() {
                                DatabaseHelper.getInstance(applicationContext).deletePreferenceUids(TUTORIAL_UID_IDENTIFIER)
                                showInfoText(getString(R.string.cockpit_tutorial_reset_confirm))
                            }
                        })
                        .setAutoCollapse(true)
                        .updateViews(true)
                val disableTutorial = SwipeButton(this, getString(R.string.cockpit_tutorial_disable))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setExecutable(object : SwipeButton.SwipeButtonExecution{
                            override fun execRight() {
                                for (imageName in applicationContext.assets.list("drawable")){
                                    DatabaseHelper.getInstance(applicationContext).write(TUTORIAL_UID_IDENTIFIER.plus(imageName.replace(PNG_FILE,NOTHING)),true,DatabaseHelper.DataMode.PREFERENCES)
                                }
                                showInfoText(getString(R.string.cockpit_tutorial_disable_confirm))
                            }
                        })
                        .setAutoCollapse(true)
                        .updateViews(true)
                val isAdmin = DatabaseHelper.getInstance(applicationContext).read(IS_ADMINISTRATOR, Boolean::class, false, DatabaseHelper.DataMode.PREFERENCES)
                val userMode = SwipeButton(this, getString(R.string.cockpit_user_mode, if (isAdmin) getString(R.string.literal_administrator) else getString(R.string.literal_user)))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setAutoCollapse(true)
                        .updateViews(true)
                userMode.setExecutable(object : SwipeButton.SwipeButtonExecution{
                            override fun execRight() {
                                val administrator = !DatabaseHelper.getInstance(applicationContext).read(IS_ADMINISTRATOR, Boolean::class, false, DatabaseHelper.DataMode.PREFERENCES)
                                DatabaseHelper.getInstance(applicationContext).write(IS_ADMINISTRATOR, administrator, DatabaseHelper.DataMode.PREFERENCES)
                                userMode.setText(getString(R.string.cockpit_user_mode, if (administrator) getString(R.string.literal_administrator) else getString(R.string.literal_user)))
                            }
                        })
                val whatIfMode = WhatIfMode.valueOf(DatabaseHelper.getInstance(applicationContext).read(WHAT_IF_MODE, String::class, WhatIfMode.ALL.toString(),DatabaseHelper.DataMode.PREFERENCES))
                val whatIfSwitch = SwipeButton(this,
                        when (whatIfMode){
                            WhatIfMode.ALL -> getString(R.string.what_if_all)
                            WhatIfMode.DYNAMIC -> getString(R.string.what_if_dynamic)
                            WhatIfMode.STAKEHOLDER -> getString(R.string.what_if_stakeholder)
                            WhatIfMode.OBJECTS -> getString(R.string.what_if_objects)
                            WhatIfMode.STATIC -> getString(R.string.what_if_static)
                            WhatIfMode.NONE -> getString(R.string.what_if_none)
                        })
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setAutoCollapse(true)
                        .updateViews(true)
                whatIfSwitch.setExecutable(object : SwipeButton.SwipeButtonExecution{
                    override fun execRight() {
                        val oldWhatIfMode = WhatIfMode.valueOf(DatabaseHelper.getInstance(applicationContext).read(WHAT_IF_MODE, String::class, WhatIfMode.ALL.toString(),DatabaseHelper.DataMode.PREFERENCES))
                        val newMode = when (oldWhatIfMode){
                            WhatIfMode.ALL -> WhatIfMode.DYNAMIC
                            WhatIfMode.DYNAMIC -> WhatIfMode.STAKEHOLDER
                            WhatIfMode.STAKEHOLDER -> WhatIfMode.OBJECTS
                            WhatIfMode.OBJECTS -> WhatIfMode.STATIC
                            WhatIfMode.STATIC -> WhatIfMode.NONE
                            WhatIfMode.NONE -> WhatIfMode.ALL
                        }
                        whatIfSwitch.setText(
                                when (newMode){
                                    WhatIfMode.ALL -> getString(R.string.what_if_all)
                                    WhatIfMode.DYNAMIC -> getString(R.string.what_if_dynamic)
                                    WhatIfMode.STAKEHOLDER -> getString(R.string.what_if_stakeholder)
                                    WhatIfMode.OBJECTS -> getString(R.string.what_if_objects)
                                    WhatIfMode.STATIC -> getString(R.string.what_if_static)
                                    WhatIfMode.NONE -> getString(R.string.what_if_none)
                                })
                        when (newMode) {
                            WhatIfMode.ALL -> showInfoText(getString(R.string.what_if_all_text))
                            WhatIfMode.DYNAMIC -> showInfoText(getString(R.string.what_if_dynamic_text))
                            WhatIfMode.STAKEHOLDER -> showInfoText(getString(R.string.what_if_stakeholder_text))
                            WhatIfMode.OBJECTS -> showInfoText(getString(R.string.what_if_objects_text))
                            WhatIfMode.STATIC -> showInfoText(getString(R.string.what_if_static_text))
                            WhatIfMode.NONE -> showInfoText(getString(R.string.what_if_none_text))
                        }
                        DatabaseHelper.getInstance(applicationContext).write(WHAT_IF_MODE,newMode.toString(),DatabaseHelper.DataMode.PREFERENCES)
                    }
                })
                val frequency = DatabaseHelper.getInstance(applicationContext).read(Constants.WHAT_IF_PROPOSAL_CHECK, Int::class, 3, DatabaseHelper.DataMode.PREFERENCES)
                val whatIfFrequency = SwipeButton(this,getString(R.string.what_if_frequency_x,frequency))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_minus, R.string.icon_plus, null, null, null)
                        .setButtonStates(true, true, false, false)
                        .setAutoCollapse(true)
                        .updateViews(true)
                whatIfFrequency.setExecutable(object : SwipeButton.SwipeButtonExecution{
                    override fun execRight() {
                        val f = DatabaseHelper.getInstance(applicationContext).read(Constants.WHAT_IF_PROPOSAL_CHECK, Int::class, 3, DatabaseHelper.DataMode.PREFERENCES)+1
                        DatabaseHelper.getInstance(applicationContext).write(Constants.WHAT_IF_PROPOSAL_CHECK, f, DatabaseHelper.DataMode.PREFERENCES)
                        if (f == 1){
                            showInfoText(getString(R.string.what_if_frequency_set_to_1))
                        }else{
                            showInfoText(getString(R.string.what_if_frequency_set,StringHelper.numberToPositionString(f)))
                        }
                        whatIfFrequency.setText(getString(R.string.what_if_frequency_x,f))
                    }
                    override fun execLeft() {
                        val f = NumberHelper.capAtLow(DatabaseHelper.getInstance(applicationContext).read(Constants.WHAT_IF_PROPOSAL_CHECK, Int::class, 3, DatabaseHelper.DataMode.PREFERENCES)-1,1)
                        DatabaseHelper.getInstance(applicationContext).write(Constants.WHAT_IF_PROPOSAL_CHECK, f, DatabaseHelper.DataMode.PREFERENCES)
                        if (f == 1){
                            showInfoText(getString(R.string.what_if_frequency_set_to_1))
                        }else{
                            showInfoText(getString(R.string.what_if_frequency_set,StringHelper.numberToPositionString(f)))
                        }
                        whatIfFrequency.setText(getString(R.string.what_if_frequency_x,f))
                    }
                })
                val whatIfWipe = SwipeButton(this,getString(R.string.what_if_wipe))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryAttention),ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setButtonStates(false, true, false, false)
                        .setAutoCollapse(true)
                        .updateViews(true)
                whatIfWipe.setExecutable(object : SwipeButton.SwipeButtonExecution{
                    override fun execRight() {
                        DatabaseHelper.getInstance(applicationContext).delete(WHAT_IF_DATA,ByteArray::class)
                        DatabaseHelper.getInstance(applicationContext).write(Constants.WHAT_IF_INITIALIZED, false, DatabaseHelper.DataMode.PREFERENCES)
                        DatabaseHelper.getInstance(applicationContext).write(Constants.WHAT_IF_PROPOSAL_COUNT, 0, DatabaseHelper.DataMode.PREFERENCES)
                        showInfoText(getString(R.string.what_if_wipe))
                    }
                })
                val sreStyle = getSreStyle(applicationContext)
                val styleSwitch = SwipeButton(this, getString(R.string.cockpit_mode_x,sreStyle))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setAutoCollapse(true)
                        .updateViews(true)
                styleSwitch.setExecutable(object : SwipeButton.SwipeButtonExecution{
                    override fun execRight() {
                        val style = getSreStyle(applicationContext)
                        var newStyle: SreStyle? = null
                        when (style){
                            SreStyle.NORMAL -> {
                                newStyle = SreStyle.CONTRAST
                                styleSwitch.setText(getString(R.string.cockpit_mode_contrast))}
                            SreStyle.CONTRAST -> {
                                newStyle = SreStyle.OLED
                                styleSwitch.setText(getString(R.string.cockpit_mode_oled))}
                            SreStyle.OLED -> {
                                newStyle = SreStyle.NORMAL
                                styleSwitch.setText(getString(R.string.cockpit_mode_normal))}
                        }
                        DatabaseHelper.getInstance(applicationContext).write(STYLE, newStyle.toString(), DatabaseHelper.DataMode.PREFERENCES)
                        if (CollectionHelper.oneOf(newStyle,SreStyle.NORMAL,SreStyle.CONTRAST)) {
                            finish()
                            intent.putExtra(IS_RELOAD,true)
                            startActivity(intent)
                        }else{
                            reStyle(applicationContext,getConfiguredRootLayout())
                            window.statusBarColor = StyleHelper.get(applicationContext).getStatusBarColor(applicationContext,newStyle)
                        }
                    }
                })
                val wipeWalkthroughs = SwipeButton(this, getString(R.string.cockpit_wipe_walkthroughs))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setExecutable(object : SwipeButton.SwipeButtonExecution{
                            override fun execRight() {
                                DatabaseHelper.getInstance(applicationContext).dropAndRecreateWalkthroughs()
                                showInfoText(getString(R.string.cockpit_wipe_walkthroughs_confirm), R.color.srePrimaryAttention)
                            }
                        })
                        .setColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryAttention),ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setAutoCollapse(true)
                        .updateViews(true)
                val wipeData = SwipeButton(this, getString(R.string.cockpit_wipe_data))
                        .setButtonMode(SwipeButton.SwipeButtonMode.DOUBLE)
                        .setButtonIcons(R.string.icon_null, R.string.icon_cogwheels, null, null, null)
                        .setButtonStates(false, true, false, false)
                        .setExecutable(object : SwipeButton.SwipeButtonExecution{
                            override fun execRight() {
                                val userName = DatabaseHelper.getInstance(applicationContext).read(Constants.USER_NAME, String::class, Constants.NOTHING)
                                DatabaseHelper.getInstance(applicationContext).dropAndRecreateAll()
                                DatabaseHelper.getInstance(applicationContext).write(Constants.USER_NAME,userName)
                                showInfoText(getString(R.string.cockpit_wipe_data_confirm), R.color.srePrimaryWarn)
                                recreateViews()
                                reStyle(applicationContext,getConfiguredRootLayout())
                            }
                        })
                        .setColors(ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn),ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled))
                        .setAutoCollapse(true)
                        .updateViews(true)
                getContentHolderLayout().addView(resetTutorial)
                getContentHolderLayout().addView(disableTutorial)
                getContentHolderLayout().addView(whatIfSwitch)
                getContentHolderLayout().addView(whatIfFrequency)
                getContentHolderLayout().addView(userMode)
                getContentHolderLayout().addView(whatIfWipe)
                getContentHolderLayout().addView(styleSwitch)
                getContentHolderLayout().addView(wipeWalkthroughs)
                getContentHolderLayout().addView(wipeData)
                tutorialOpen = SreTutorialLayoutDialog(this@CockpitActivity,screenWidth,"info_functions").addEndExecutable { tutorialOpen = false }.show(tutorialOpen)
            }
        }
    }

    private fun generatePermissionExecutable(permission: PermissionHelper.Companion.PermissionGroups, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                if (!PermissionHelper.check(this@CockpitActivity, permission)) {
                    PermissionHelper.request(this@CockpitActivity, permission)
                    activeButton = button
                }
            }
            override fun execLeft() {
                showInfoText(permission.getDescription(applicationContext))
            }
        }
    }

    private fun generateCommunicationExecutable(communication: CommunicationHelper.Companion.Communications, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                if (!CommunicationHelper.check(this@CockpitActivity, communication)) {
                    val active = CommunicationHelper.toggle(this@CockpitActivity, communication)
                    button.setIndividualButtonColors(if (active) getColorWithStyle(applicationContext,R.color.srePrimaryPastel) else ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn), if (active) getColorWithStyle(applicationContext,R.color.srePrimarySafe) else getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled)).updateViews(false)
                }
            }

            override fun execLeft() {
                if (CommunicationHelper.check(this@CockpitActivity, communication)) {
                    val active = CommunicationHelper.toggle(this@CockpitActivity, communication)
                    button.setIndividualButtonColors(if (active) getColorWithStyle(applicationContext,R.color.srePrimaryPastel) else ContextCompat.getColor(applicationContext,R.color.srePrimaryWarn), if (active) getColorWithStyle(applicationContext,R.color.srePrimarySafe) else getColorWithStyle(applicationContext,R.color.srePrimaryPastel), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled), ContextCompat.getColor(applicationContext,R.color.srePrimaryDisabled)).updateViews(false)
                }
            }

            override fun execReset() {
                getInfoContent().text = mode.getDescription(applicationContext)
            }
        }
    }

    private fun generateSensorExecutable(sensor: Sensor, button: SwipeButton): SwipeButton.SwipeButtonExecution {
        return object : SwipeButton.SwipeButtonExecution {
            override fun execRight() {
                SensorHelper.getInstance(applicationContext).registerTextGraphListener(sensor, getInfoContent())
                getInfoTitle().text = resources.getString(R.string.observing,SensorHelper.getInstance(applicationContext).getTypeName(sensor.name))
            }

            override fun execLeft() {
                SensorHelper.getInstance(applicationContext).unregisterTextGraphListener()
                getInfoTitle().text = StringHelper.styleString(getSpannedStringFromId(getConfiguredInfoString()), fontAwesome)
            }

            override fun execReset() {
                getInfoContent().text = mode.getDescription(applicationContext)
            }
        }
    }
}