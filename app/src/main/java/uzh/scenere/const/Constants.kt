package uzh.scenere.const

class Constants {
    companion object {
        const val APPLICATION_ID: String = "uzh.scenere.SceneRe"
        //Kotlin
        const val REFLECTION: String = " (Kotlin reflection is not available)"
        //Android
        const val PHONE_STATE: String = "android.intent.action.PHONE_STATE"
        const val NEW_OUTGOING_CALL: String = "android.intent.action.NEW_OUTGOING_CALL"
        const val SMS_RECEIVED: String = "android.provider.Telephony.SMS_RECEIVED"
        //Storage
        const val SHARED_PREFERENCES: String = "scenereSharedPreferences"
        const val USER_NAME: String = "scenereUserName"
        const val IS_RELOAD: String = "scenereReload"
        const val IS_ADMINISTRATOR: String = "scenereIsAdministrator"
        const val USER_ID: String = "scenereUserId"
        const val STYLE: String = "scenereStyle"
        const val WHAT_IF_MODE: String = "scenereWhatIfMode"
        const val WHAT_IF_INITIALIZED: String = "scenereWhatIfInitialized"
        const val WHAT_IF_PROPOSAL_COUNT: String = "scenereWhatIfProposalCount"
        const val WHAT_IF_PROPOSAL_CHECK: String = "scenereWhatIfProposalCheck"
        const val WHAT_IF_DATA: String = "scenereWhatIfData"
        const val WHAT_IF_SNAPSHOT_YOUNG_DATA: String = "scenereWhatIfDataSnapshot1"
        const val WHAT_IF_SNAPSHOT_INTERMEDIATE_DATA: String = "scenereWhatIfDataSnapshot2"
        const val WHAT_IF_SNAPSHOT_OLD_DATA: String = "scenereWhatIfDataSnapshot3"
        const val IMPORT_FOLDER: String = "scenereImportFolder"
        const val ANONYMOUS: String = "John Doe"
        const val WALKTHROUGH_PLAY_STATE: String = "scenereWalkthroughPlayState"
        const val WALKTHROUGH_PLAY_STATE_SHORTCUT: String = "scenereWalkthroughPlayStateShortcut"
        //NUMBERS
        val COORDINATES_PATTERN: Regex = "-?[1]?[0-9]{1,2}\\.[0-9]{1,6},-?[1]?[0-9]{1,2}\\.[0-9]{1,6}".toRegex()
        val ICON_PATTERN: Regex = ".*[^\\x20-\\x7E].*".toRegex()
        const val EARTH_RADIUS_M: Double = 6371000.0
        const val COLOR_BOUND: Int = 16777215
        const val MILLION_D: Double = 1000000.0
        const val MILLION: Int = 1000000
        const val WIFI_DIRECT_PORT: Int = 8118
        const val FIVE_MIN_MS: Long = 300000
        const val TEN_SEC_MS: Long = 10000
        const val FIVE_SEC_MS: Long = 5000
        const val TWO_SEC_MS: Long = 2000
        const val ONE_SEC_MS: Long = 1000
        const val HALF_SEC_MS: Long = 500
        const val THIRD_SEC_MS: Long = 300
        const val THOUSAND: Double = 1000.0
        const val HUNDRED: Double = 100.0
        const val EIGHT_KB: Int = 8192
        const val ONE: Int = 1
        const val FRACTION: Float = 0.1f
        const val ZERO_S: String = "0"
        const val ZERO_D: Double = 0.0
        const val ZERO_F: Float = 0.0f
        const val ZERO_L: Long = 0
        const val ZERO: Int = 0
        const val SEC_MS: Long = 1000
        const val MIN_MS: Long = 60000
        const val HOUR_MS: Long = 3600000
        const val DAY_MS: Long = 86400000
        //PERMISSIONS
        const val PERMISSION_REQUEST_ALL: Int = 888
        const val PERMISSION_REQUEST_GPS: Int = 666
        const val UNKNOWN_RESPONSE: Int = 404
        //REQUESTS
        const val IMPORT_DATA_FILE: Int = 111
        const val IMPORT_DATA_FOLDER: Int = 222
        //BUNDLE
        const val BUNDLE_GLOSSARY_TOPIC: String = "sreGlossaryTopic"
        //VALIDATION
        const val NOT_VALIDATED: Int = 0
        const val VALIDATION_OK: Int = 1
        const val VALIDATION_EMPTY: Int = 2
        const val VALIDATION_FAILED: Int = 3
        const val VALIDATION_INVALID: Int = 4
        const val VALIDATION_NO_DATA: Int = 5
        //TAGS
        const val GENERAL_TAG: String = "SRE-TAG"
        const val BUNDLE_GLOSSARY_ADDITIONAL_TOPICS: String = "sreGlossaryAdditionalTopics"
        const val BUNDLE_PROJECT: String = "sreBundleProject"
        const val BUNDLE_SCENARIO: String = "sreBundleScenario"
        const val BUNDLE_OBJECT: String = "sreBundleObject"
        const val LAST_USED_LOCATION: String = "sreLastUsedLocation"
        const val LAST_KNOWN_LOCATION: String = "sreLastKnownLocation"
        //UID-IDENTIFIERS
        const val PROJECT_UID_IDENTIFIER: String = "project_"
        const val STAKEHOLDER_UID_IDENTIFIER: String = "stakeholder_"
        const val OBJECT_UID_IDENTIFIER: String = "object_"
        const val SCENARIO_UID_IDENTIFIER: String = "scenario_"
        const val ATTRIBUTE_UID_IDENTIFIER: String = "attribute_"
        const val PATH_UID_IDENTIFIER: String = "path_"
        const val ELEMENT_UID_IDENTIFIER: String = "element_"
        const val WALKTHROUGH_UID_IDENTIFIER: String = "walkthrough_"
        const val TUTORIAL_UID_IDENTIFIER: String = "tutorial_"
        const val TARGET_STEP_UID_IDENTIFIER: String = "target_step_"
        const val CHANGE_UID_IDENTIFIER: String = "change_"
        const val CHECK_VALUE_UID_IDENTIFIER: String = "check_value_"
        const val CHECK_MODE_UID_IDENTIFIER: String = "check_mode_"
        const val HASH_MAP_UID_IDENTIFIER: String = "hash_map_"
        const val HASH_MAP_OPTIONS_IDENTIFIER: String = "hash_map_options_"
        const val HASH_MAP_LINK_IDENTIFIER: String = "hash_map_link_"
        const val ARRAY_LIST_WHAT_IF_IDENTIFIER: String = "array_list_what_if_"
        const val VERSIONING_IDENTIFIER: String = "version_"
        const val MIN_IDENTIFIER: String = "min_"
        const val MAX_IDENTIFIER: String = "max_"
        const val INIT_IDENTIFIER: String = "init_"
        const val OUTRO_IDENTIFIER: String = "outro_"
        const val INTRO_IDENTIFIER: String = "intro_"
        //GENERAL
        const val NEW_LINE_C: Char = '\n'
        const val SPACE_C: Char = ' '
        const val EQUALS: String = "="
        const val NEW_LINE: String = "\n"
        const val CARRIAGE_RETURN: String = "\r"
        const val NEW_LINE_TOKEN: String = "!!NEW_LINE_TOKEN!!"
        const val COMMA_TOKEN: String = "!!COMMA!!"
        const val REPLACEMENT_TOKEN: String = "!!REPLACEMENT!!"
        //WHAT IF TOKEN
        const val STAKEHOLDER_1_TOKEN: String = "!!STAKEHOLDER_1!!"
        const val STAKEHOLDER_2_TOKEN: String = "!!STAKEHOLDER_2!!"
        const val OBJECT_TOKEN: String = "!!OBJECT!!"
        const val ATTRIBUTE_TOKEN: String = "!!ATTRIBUTE!!"
        const val STATIC_TOKEN: String = "!!STATIC!!"
        //COMBINATIONS
        const val S1_S2_TOKEN: String = "$STAKEHOLDER_1_TOKEN$STAKEHOLDER_2_TOKEN"
        const val S1_S2_O_TOKEN: String = "$STAKEHOLDER_1_TOKEN$STAKEHOLDER_2_TOKEN$OBJECT_TOKEN"
        const val S1_S2_A_TOKEN: String = "$STAKEHOLDER_1_TOKEN$STAKEHOLDER_2_TOKEN$ATTRIBUTE_TOKEN"
        const val S1_S2_O_A_TOKEN: String = "$STAKEHOLDER_1_TOKEN$STAKEHOLDER_2_TOKEN$OBJECT_TOKEN$ATTRIBUTE_TOKEN"
        const val S1_O_TOKEN: String = "$STAKEHOLDER_1_TOKEN$OBJECT_TOKEN"
        const val S1_A_TOKEN: String = "$STAKEHOLDER_1_TOKEN$ATTRIBUTE_TOKEN"
        const val S1_O_A_TOKEN: String = "$STAKEHOLDER_1_TOKEN$OBJECT_TOKEN$ATTRIBUTE_TOKEN"
        const val O_A_TOKEN: String = "$OBJECT_TOKEN$ATTRIBUTE_TOKEN"

        const val PERCENT: String = "%"
        const val SPACE: String = " "
        const val COMMA: String = ","
        const val COMMA_DELIM: String = ", "
        const val SEMI_COLON: String = ";"
        const val DOLLAR_STRING: Char = '$'
        const val NOTHING: String = ""
        const val QUOTE: Char = '"'
        const val BRACKET_OPEN: Char = '<'
        const val BRACKET_CLOSE: Char = '>'
        const val DASH: String = "-"
        const val GROUND_DASH: String = "_"
        const val SLASH: String = "/"
        const val BOLD_START: String = "<b>"
        const val BOLD_END: String = "</b>"
        const val BREAK: String = "<br>"
        const val ARROW_RIGHT: String = "->"
        const val ARROW_LEFT: String = "<-"
        const val NONE: String = "None"
        const val NO_DATA: String = "No Data"
        const val MASTER: String = "MASTER"
        const val TRY_AGAIN: String = "Connection invalid, try again on both Devices!"
        const val CONNECTION_ESTABLISHED: String = "Connection established, please wait!"
        const val NOT_CONSULTED: String = "Not consulted"
        //XML
        const val EMPTY_LIST: String = "EmptyList"
        const val NULL: String = "NULL"
        const val NULL_CLASS: String = "Null"
        const val HYPHEN: String = "-"
        const val STRING: String = "String"
        const val INT: String = "Int"
        const val FLOAT: String = "Float"
        const val LONG: String = "Long"
        const val DOUBLE: String = "Double"
        const val BOOLEAN: String = "Boolean"
        const val HASH_MAP_ENTRY: String = "HashMap\$HashMapEntry"
        //INPUT CONFIG
        const val SINGLE_SELECT: String = "singleSelect"
        const val SINGLE_SELECT_WITH_PRESET_POSITION: String = "singleSelect="
        const val READ_ONLY: String = "readOnly"
        const val COMPLETE_REMOVAL_DISABLED: String = "completeRemovalDisabled"
        const val COMPLETE_REMOVAL_DISABLED_WITH_PRESET: String = "completeRemovalDisabled="
        const val SIMPLE_LOOKUP: String = "simpleLookup"
        //FILE FORMATS
        const val PNG_FILE: String = ".png"
        const val SRE_FILE: String = ".sre"
        //FILE LOCATIONS
        const val FOLDER_ROOT: String = "/SceneRe"
        const val FOLDER_ANALYTICS: String = "/Analytics"
        const val FOLDER_EXPORT: String = "/Export"
        const val FOLDER_IMPORT: String = "/Import"
        const val FOLDER_DATABASE: String = "/Database"
        const val FOLDER_TEMP: String = "/Temp"
        //ELEMENTS
        const val STARTING_POINT: String = "starting_point"
        //ATTRIBUTE TYPES
        const val TYPE_OBJECT: String = "Object"
        const val TYPE_RESOURCE: String = "Resource"
        const val TYPE_STANDARD_STEP: String = "StandardStep"
        const val TYPE_JUMP_STEP: String = "JumpStep"
        const val TYPE_SOUND_STEP: String = "SoundStep"
        const val TYPE_VIBRATION_STEP: String = "VibrationStep"
        const val TYPE_RESOURCE_STEP: String = "ResourceStep"
        const val TYPE_BUTTON_TRIGGER: String = "ButtonTrigger"
        const val TYPE_IF_ELSE_TRIGGER: String = "IfElseTrigger"
        const val TYPE_STAKEHOLDER_INTERACTION_TRIGGER: String = "StakeholderInteractionTrigger"
        const val TYPE_TIME_TRIGGER: String = "TimeTrigger"
        const val TYPE_INPUT_TRIGGER: String = "InputTrigger"
        const val TYPE_RESOURCE_CHECK_TRIGGER: String = "ResourceCheckTrigger"
        const val TYPE_SOUND_TRIGGER: String = "SoundTrigger"
        const val TYPE_BLUETOOTH_TRIGGER: String = "BluetoothTrigger"
        const val TYPE_GPS_TRIGGER: String = "GpsTrigger"
        const val TYPE_MOBILE_NETWORK_TRIGGER: String = "MobileNetworkTrigger"
        const val TYPE_NFC_TRIGGER: String = "NfcTrigger"
        const val TYPE_WIFI_TRIGGER: String = "WifiTrigger"
        const val TYPE_CALL_TRIGGER: String = "CallTrigger"
        const val TYPE_SMS_TRIGGER: String = "SmsTrigger"
        const val TYPE_ACCELERATION_TRIGGER: String = "AccelerationTrigger"
        const val TYPE_GYROSCOPE_TRIGGER: String = "GyroscopeTrigger"
        const val TYPE_LIGHT_TRIGGER: String = "LightTrigger"
        const val TYPE_MAGNETOMETER_TRIGGER: String = "MagnetometerTrigger"
        //PDF,CSV
        const val ANALYTICS_EXPORT_NAME = "SceneRE-Analytics-Export_"
        const val LIST_BEGIN = "- "
        const val FONT_HELVETICA_NORMAL = "Helvetica"
        const val FONT_HELVETICA_BOLD = "Helvetica-Bold"
        const val FILE_TYPE_TFF = ".ttf"
        const val FILE_TYPE_TXT = ".txt"
        const val FILE_TYPE_PNG = ".png"
        const val FILE_TYPE_CSV = ".csv"
        const val PART_TOKEN = "<part_"
        const val BREAK_TOKEN = "<breakbelow_"
        const val MARGIN_TOKEN = "<margin_"
        const val TABLE_TOKEN = "<table_"
        const val BOLD_TOKEN = "<b_"
        const val IMAGE_TOKEN = "<image_"

        //COLORS
        const val CRIMSON: String = "#DC143C"
        const val DARK_RED: String = "#8B0000"
        const val GOLD: String = "#FED73B"
        const val MATERIAL_100_RED: String = "#F8BBD0"
        const val MATERIAL_100_VIOLET: String = "#E1BEE7"
        const val MATERIAL_100_BLUE: String = "#C5CAE9"
        const val MATERIAL_100_TURQUOISE: String = "#BBDEFB"
        const val MATERIAL_100_GREEN: String = "#C8E6C9"
        const val MATERIAL_100_LIME: String = "#F0F4C3"
        const val MATERIAL_100_YELLOW: String = "#FFF9C4"
        const val MATERIAL_100_ORANGE: String = "#FFCCBC"

        const val MATERIAL_700_RED: String = "#D32F2F"
        const val MATERIAL_700_VIOLET: String = "#7B1FA2"
        const val MATERIAL_700_BLUE: String = "#303F9F"
        const val MATERIAL_700_TURQUOISE: String = "#0097A7"
        const val MATERIAL_700_GREEN: String = "#388E3C"
        const val MATERIAL_700_LIME: String = "#AFB42B"
        const val MATERIAL_700_YELLOW: String = "#FBC02D"
        const val MATERIAL_700_ORANGE: String = "#E64A19"
    }
}