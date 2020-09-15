package uzh.scenere.datamodel.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.ARRAY_LIST_WHAT_IF_IDENTIFIER
import uzh.scenere.const.Constants.Companion.CHANGE_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.CHECK_MODE_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.CHECK_VALUE_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.COMMA
import uzh.scenere.const.Constants.Companion.GROUND_DASH
import uzh.scenere.const.Constants.Companion.HASH_MAP_LINK_IDENTIFIER
import uzh.scenere.const.Constants.Companion.HASH_MAP_OPTIONS_IDENTIFIER
import uzh.scenere.const.Constants.Companion.INIT_IDENTIFIER
import uzh.scenere.const.Constants.Companion.MAX_IDENTIFIER
import uzh.scenere.const.Constants.Companion.MIN_IDENTIFIER
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.TARGET_STEP_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.TYPE_ACCELERATION_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_BLUETOOTH_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_BUTTON_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_CALL_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_GPS_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_GYROSCOPE_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_IF_ELSE_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_INPUT_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_JUMP_STEP
import uzh.scenere.const.Constants.Companion.TYPE_LIGHT_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_MAGNETOMETER_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_NFC_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_OBJECT
import uzh.scenere.const.Constants.Companion.TYPE_RESOURCE
import uzh.scenere.const.Constants.Companion.TYPE_RESOURCE_CHECK_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_RESOURCE_STEP
import uzh.scenere.const.Constants.Companion.TYPE_SMS_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_SOUND_STEP
import uzh.scenere.const.Constants.Companion.TYPE_SOUND_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_STAKEHOLDER_INTERACTION_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_STANDARD_STEP
import uzh.scenere.const.Constants.Companion.TYPE_TIME_TRIGGER
import uzh.scenere.const.Constants.Companion.TYPE_VIBRATION_STEP
import uzh.scenere.const.Constants.Companion.TYPE_WIFI_TRIGGER
import uzh.scenere.const.Constants.Companion.VERSIONING_IDENTIFIER
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.*
import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.communication.BluetoothTrigger
import uzh.scenere.datamodel.triggers.communication.GpsTrigger
import uzh.scenere.datamodel.triggers.communication.NfcTrigger
import uzh.scenere.datamodel.triggers.communication.WifiTrigger
import uzh.scenere.datamodel.triggers.direct.*
import uzh.scenere.datamodel.triggers.indirect.CallTrigger
import uzh.scenere.datamodel.triggers.indirect.SmsTrigger
import uzh.scenere.datamodel.triggers.indirect.SoundTrigger
import uzh.scenere.datamodel.triggers.indirect.TimeTrigger
import uzh.scenere.datamodel.triggers.sensor.AccelerationTrigger
import uzh.scenere.datamodel.triggers.sensor.GyroscopeTrigger
import uzh.scenere.datamodel.triggers.sensor.LightTrigger
import uzh.scenere.datamodel.triggers.sensor.MagnetometerTrigger
import uzh.scenere.helpers.*
import java.lang.Exception
import java.util.*

class SreDatabase private constructor(val context: Context) : AbstractSreDatabase() {
    private var dbHelper: DbHelper = DbHelper(context)
    var disableNewVersion = false
    private var db: SQLiteDatabase? = null
    private val activeCursors = ArrayList<Cursor>()

    fun openReadable(){
        if (db == null){
            db = dbHelper.readableDatabase
        }
    }

    fun openWritable(){
        if (db == null){
        db = dbHelper.writableDatabase
        }
    }

    fun close(){
        if (db != null) {
            if (db!!.inTransaction()){
                db?.endTransaction()
            }
            for (cursor in activeCursors){
                if (!cursor.isClosed){
                    cursor.close()
                }
            }
            activeCursors.clear()
            db?.close()
            db = null
        }
    }

    private fun getDb(): SQLiteDatabase{
        return db!!
    }

    val map = TreeMap<Long,String>()
    var lastInsert = 0L
    var lastStatement = NOTHING
    private fun getCursor(table: String, columns: Array<String>, selection: String, selectionArgs: Array<String>?,
                          groupBy: String?, having: String?, orderBy: String?, limit: String?): Cursor {
        val cursor = getDb().query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)
        if (lastInsert != 0L){
            map[System.currentTimeMillis()-lastInsert] = lastStatement
        }
        lastInsert = System.currentTimeMillis()
        lastStatement = table+selection
        activeCursors.add(cursor)
        return cursor
    }

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: SreDatabase? = null

        fun getInstance(context: Context): SreDatabase {
            return when {
                instance != null -> instance!!
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = SreDatabase(context)
                    }
                    instance!!
                }
            }
        }
    }

    /** WRITE    **
     ** TO       **
     ** DATABASE **/
    fun writeByteArray(key: String, value: ByteArray): Long {
        val values = ContentValues()
        values.put(DataTableEntry.KEY, key)
        values.put(DataTableEntry.VALUE, value)
        return insert(DataTableEntry.TABLE_NAME, DataTableEntry.KEY, key, values)
    }

    fun writeString(key: String, value: String): Long {
        val values = ContentValues()
        values.put(TextTableEntry.KEY, key)
        values.put(TextTableEntry.VALUE, value)
        return insert(TextTableEntry.TABLE_NAME, TextTableEntry.KEY, key, values)
    }

    fun writeBoolean(key: String, value: Boolean): Long {
        val values = ContentValues()
        values.put(DataTableEntry.KEY, key)
        values.put(DataTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeShort(key: String, value: Short): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeInt(key: String, value: Int): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeLong(key: String, value: Long): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeFloat(key: String, value: Float): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeDouble(key: String, value: Double): Long {
        val values = ContentValues()
        values.put(NumberTableEntry.KEY, key)
        values.put(NumberTableEntry.VALUE, value)
        return insert(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key, values)
    }

    fun writeProject(project: Project): Long {
        addVersioning(project.id, project.changeTimeMs)
        val values = ContentValues()
        values.put(ProjectTableEntry.ID, project.id)
        values.put(ProjectTableEntry.CREATOR, project.creator)
        values.put(ProjectTableEntry.TITLE, project.title)
        values.put(ProjectTableEntry.DESCRIPTION, project.description)
        return insert(ProjectTableEntry.TABLE_NAME, ProjectTableEntry.ID, project.id, values)
    }

    fun writeStakeholder(stakeholder: Stakeholder): Long {
        addVersioning(stakeholder.id, stakeholder.changeTimeMs)
        val values = ContentValues()
        values.put(StakeholderTableEntry.ID, stakeholder.id)
        values.put(StakeholderTableEntry.PROJECT_ID, stakeholder.projectId)
        values.put(StakeholderTableEntry.NAME, stakeholder.name)
        values.put(StakeholderTableEntry.DESCRIPTION, stakeholder.description)
        return insert(StakeholderTableEntry.TABLE_NAME, StakeholderTableEntry.ID, stakeholder.id, values)
    }

    fun writeObject(obj: AbstractObject): Long {
        addVersioning(obj.id, obj.changeTimeMs)
        val values = ContentValues()
        values.put(ObjectTableEntry.ID, obj.id)
        values.put(ObjectTableEntry.SCENARIO_ID, obj.scenarioId)
        values.put(ObjectTableEntry.NAME, obj.name)
        values.put(ObjectTableEntry.DESCRIPTION, obj.description)
        values.put(ObjectTableEntry.IS_RESOURCE, obj.isResource)
        for (attribute in obj.attributes) {
            writeAttribute(attribute)
        }
        if (obj is Resource) {
            writeInt(MIN_IDENTIFIER.plus(obj.id), obj.min)
            writeInt(MAX_IDENTIFIER.plus(obj.id), obj.max)
            writeInt(INIT_IDENTIFIER.plus(obj.id), obj.init)
        }
        return insert(ObjectTableEntry.TABLE_NAME, ObjectTableEntry.ID, obj.id, values)
    }

    fun writeAttribute(attribute: Attribute): Long {
        addVersioning(attribute.getVersioningId(), attribute.changeTimeMs)
        val values = ContentValues()
        values.put(AttributeTableEntry.ID, attribute.id)
        values.put(AttributeTableEntry.REF_ID, attribute.refId)
        values.put(AttributeTableEntry.KEY, attribute.key)
        values.put(AttributeTableEntry.VALUE, attribute.value)
        values.put(AttributeTableEntry.TYPE, attribute.type)
        return insert(AttributeTableEntry.TABLE_NAME, AttributeTableEntry.ID, attribute.id, values)
    }

    fun writeScenario(scenario: Scenario): Long {
        addVersioning(scenario.id, scenario.changeTimeMs)
        val values = ContentValues()
        values.put(ScenarioTableEntry.ID, scenario.id)
        values.put(ScenarioTableEntry.PROJECT_ID, scenario.projectId)
        values.put(ScenarioTableEntry.TITLE, scenario.title)
        values.put(ScenarioTableEntry.INTRO, scenario.intro)
        values.put(ScenarioTableEntry.OUTRO, scenario.outro)
        return insert(ScenarioTableEntry.TABLE_NAME, ScenarioTableEntry.ID, scenario.id, values)
    }

    fun writeElement(element: IElement): Long {
        val values = ContentValues()
        values.put(ElementTableEntry.ID, element.getElementId())
        values.put(ElementTableEntry.PREV_ID, element.getPreviousElementId())
        values.put(ElementTableEntry.PATH_ID, element.getElementPathId())
        var time = 0L
        if (element is AbstractStep) {
            time = addVersioning(element.id, element.changeTimeMs)
            values.put(ElementTableEntry.TITLE, element.title)
            values.put(ElementTableEntry.TEXT, element.text)
            writeByteArray(ARRAY_LIST_WHAT_IF_IDENTIFIER.plus(element.getElementId()), DataHelper.toByteArray(element.whatIfs))
            for (obj in element.objects) {
                val attribute = Attribute.AttributeBuilder(element.id, obj.id, null).withAttributeType(TYPE_OBJECT).buildAsLink()
                attribute.changeTimeMs = time
                writeAttribute(attribute)
            }
        } else if (element is AbstractTrigger) {
            addVersioning(element.id, element.changeTimeMs)
        }
        when (element) {
            //STEPS
            is StandardStep -> {
                values.put(ElementTableEntry.TYPE, TYPE_STANDARD_STEP)
            }
            is JumpStep -> {
                writeString(TARGET_STEP_UID_IDENTIFIER.plus(element.getElementId()), element.targetStepId!!)
                values.put(ElementTableEntry.TYPE, TYPE_JUMP_STEP)
            }
            is SoundStep -> {
                values.put(ElementTableEntry.TYPE, TYPE_SOUND_STEP)
            }
            is VibrationStep -> {
                values.put(ElementTableEntry.TYPE, TYPE_VIBRATION_STEP)
            }
            is ResourceStep -> {
                writeInt(CHANGE_UID_IDENTIFIER.plus(element.getElementId()), element.change)
                values.put(ElementTableEntry.TYPE, TYPE_RESOURCE_STEP)
                val attribute = Attribute.AttributeBuilder(element.id, element.resource!!.id, null).withAttributeType(TYPE_RESOURCE).buildAsLink()
                attribute.changeTimeMs = time
                writeAttribute(attribute)
            }
            //TRIGGERS
            is ButtonTrigger -> {
                values.put(ElementTableEntry.TITLE, element.buttonLabel)
                values.put(ElementTableEntry.TYPE, TYPE_BUTTON_TRIGGER)
            }
            is IfElseTrigger -> {
                values.put(ElementTableEntry.TITLE, element.defaultOption) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_IF_ELSE_TRIGGER)
                writeByteArray(HASH_MAP_OPTIONS_IDENTIFIER.plus(element.getElementId()), DataHelper.toByteArray(element.pathOptions))
                writeByteArray(HASH_MAP_LINK_IDENTIFIER.plus(element.getElementId()), DataHelper.toByteArray(element.optionLayerLink))
            }
            is StakeholderInteractionTrigger -> {
                values.put(ElementTableEntry.TITLE, element.interactedStakeholderId) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_STAKEHOLDER_INTERACTION_TRIGGER)
            }
            is InputTrigger -> {
                values.put(ElementTableEntry.TITLE, element.input) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_INPUT_TRIGGER)
            }
            is ResourceCheckTrigger -> {
                values.put(ElementTableEntry.TITLE, element.buttonLabel)
                values.put(ElementTableEntry.TEXT, element.falseStepId) //Carrier
                values.put(ElementTableEntry.TYPE, TYPE_RESOURCE_CHECK_TRIGGER)
                writeInt(CHECK_VALUE_UID_IDENTIFIER.plus(element.getElementId()), element.checkValue)
                writeString(CHECK_MODE_UID_IDENTIFIER.plus(element.getElementId()), element.mode.toString())
                val attribute = Attribute.AttributeBuilder(element.id, element.resource!!.id, null).withAttributeType(TYPE_RESOURCE).buildAsLink()
                attribute.changeTimeMs = time
                writeAttribute(attribute)
            }
            is TimeTrigger -> {
                values.put(ElementTableEntry.TITLE, element.timeMs.toString()) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_TIME_TRIGGER)
            }
            is SoundTrigger -> {
                values.put(ElementTableEntry.TITLE, element.dB.toString()) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_SOUND_TRIGGER)
            }
            is BluetoothTrigger -> {
                values.put(ElementTableEntry.TITLE, element.deviceId) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_BLUETOOTH_TRIGGER)
            }
            is GpsTrigger -> {
                values.put(ElementTableEntry.TITLE, element.gpsData) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_GPS_TRIGGER)
            }
            is NfcTrigger -> {
                values.put(ElementTableEntry.TITLE, element.message)
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_NFC_TRIGGER)
            }
            is WifiTrigger -> {
                values.put(ElementTableEntry.TITLE, element.ssidAndStrength) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_WIFI_TRIGGER)
            }
            is CallTrigger -> {
                values.put(ElementTableEntry.TITLE, element.telephoneNr) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_CALL_TRIGGER)
            }
            is SmsTrigger -> {
                values.put(ElementTableEntry.TITLE, element.telephoneNr) //Carrier
                values.put(ElementTableEntry.TEXT, element.text)
                values.put(ElementTableEntry.TYPE, TYPE_SMS_TRIGGER)
            }
            is AccelerationTrigger -> {/*TODO*/
            }
            is GyroscopeTrigger -> {/*TODO*/
            }
            is LightTrigger -> {/*TODO*/
            }
            is MagnetometerTrigger -> {/*TODO*/
            }
        }
        return insert(ElementTableEntry.TABLE_NAME, ElementTableEntry.ID, element.getElementId(), values)
    }

    fun writePath(path: Path): Long {
        addVersioning(path.id, path.changeTimeMs)
        val values = ContentValues()
        values.put(PathTableEntry.ID, path.id)
        values.put(PathTableEntry.SCENARIO_ID, path.scenarioId)
        values.put(PathTableEntry.STAKEHOLDER_ID, path.stakeholder.id)
        values.put(PathTableEntry.LAYER, path.layer)
        for (entry in path.elements.entries) {
            writeElement(entry.value)
        }
        return insert(PathTableEntry.TABLE_NAME, PathTableEntry.ID, path.id, values)
    }

    fun writeWalkthrough(walkthrough: Walkthrough): Long {
        addVersioning(walkthrough.id, walkthrough.changeTimeMs) //2ddaff99-604b-4a24-b44f-accca02b9b9e
        val values = ContentValues()
        values.put(WalkthroughTableEntry.ID, walkthrough.id)
        values.put(WalkthroughTableEntry.SCENARIO_ID, walkthrough.scenarioId)
        values.put(WalkthroughTableEntry.STAKEHOLDER_ID, walkthrough.stakeholder.id)
        values.put(WalkthroughTableEntry.OWNER, walkthrough.owner)
        values.put(WalkthroughTableEntry.XML_DATA, walkthrough.getXml())
        return insert(WalkthroughTableEntry.TABLE_NAME, WalkthroughTableEntry.ID, walkthrough.id, values)
    }

    /** READ     **
     ** FROM     **
     ** DATABASE **/
    fun readByteArray(key: String, valueIfNull: ByteArray): ByteArray {
        val cursor = getCursor(DataTableEntry.TABLE_NAME, arrayOf(DataTableEntry.VALUE), DataTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var bytes: ByteArray? = null
        if (cursor.moveToFirst()) {
            bytes = cursor.getBlob(0)
        }
        cursor.close()
        return ObjectHelper.nvl(bytes, valueIfNull)
    }

    fun readString(key: String, valueIfNull: String): String {
        val cursor = getCursor(TextTableEntry.TABLE_NAME, arrayOf(TextTableEntry.VALUE), TextTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var d: String? = null
        if (cursor.moveToFirst()) {
            d = cursor.getString(0)
        }
        cursor.close()
        return ObjectHelper.nvl(d, valueIfNull)
    }

    fun readBoolean(key: String, valueIfNull: Boolean): Boolean {
        val cursor = getCursor(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var b: Int? = null
        if (cursor.moveToFirst()) {
            b = cursor.getInt(0)
        }
        cursor.close()
        return ObjectHelper.nvl(NumberHelper.nvl(b, 0) == 1, valueIfNull)
    }

    fun readShort(key: String, valueIfNull: Short): Short {
        val cursor = getCursor(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var s: Short? = null
        if (cursor.moveToFirst()) {
            s = cursor.getShort(0)
        }
        cursor.close()
        return NumberHelper.nvl(s, valueIfNull)
    }

    fun readInt(key: String, valueIfNull: Int): Int {
        val cursor = getCursor(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var i: Int? = null
        if (cursor.moveToFirst()) {
            i = cursor.getInt(0)
        }
        cursor.close()
        return NumberHelper.nvl(i, valueIfNull)
    }

    fun readLong(key: String, valueIfNull: Long): Long {
        val cursor = getCursor(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var l: Long? = null
        if (cursor.moveToFirst()) {
            l = cursor.getLong(0)
        }
        cursor.close()
        return NumberHelper.nvl(l, valueIfNull)
    }

    fun readFloat(key: String, valueIfNull: Float): Float {
        val cursor = getCursor(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var f: Float? = null
        if (cursor.moveToFirst()) {
            f = cursor.getFloat(0)
        }
        cursor.close()
        return NumberHelper.nvl(f, valueIfNull)
    }

    fun readDouble(key: String, valueIfNull: Double): Double {
        val cursor = getCursor(NumberTableEntry.TABLE_NAME, arrayOf(NumberTableEntry.VALUE), NumberTableEntry.KEY + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        var d: Double? = null
        if (cursor.moveToFirst()) {
            d = cursor.getDouble(0)
        }
        cursor.close()
        return NumberHelper.nvl(d, valueIfNull)
    }

    fun readProjects(): List<Project> {
        val cursor = getCursor(ProjectTableEntry.TABLE_NAME, arrayOf(ProjectTableEntry.ID, ProjectTableEntry.CREATOR, ProjectTableEntry.TITLE, ProjectTableEntry.DESCRIPTION), ONE + EQ + ONE, null, null, null, null, null)
        val projects = ArrayList<Project>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val creator = cursor.getString(1)
                val title = cursor.getString(2)
                val description = cursor.getString(3)
                val project = Project.ProjectBuilder(id, creator, title, description).build()
                project.changeTimeMs = readVersioning(id)
                projects.add(project)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return projects
    }

    fun readProject(key: String, valueIfNull: Project, fullLoad: Boolean = false): Project {
        val cursor = getCursor(ProjectTableEntry.TABLE_NAME, arrayOf(ProjectTableEntry.ID, ProjectTableEntry.CREATOR, ProjectTableEntry.TITLE, ProjectTableEntry.DESCRIPTION), ProjectTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val creator = cursor.getString(1)
            val title = cursor.getString(2)
            val description = cursor.getString(3)
            val projectBuilder = Project.ProjectBuilder(id, creator, title, description)
            if (fullLoad) {
                projectBuilder.addStakeholders(stakeholder = *readStakeholder(projectBuilder.build()).toTypedArray())
                projectBuilder.addScenarios(scenario = *readScenarios(projectBuilder.build(), true).toTypedArray())
            }
            val project = projectBuilder.build()
            project.changeTimeMs = readVersioning(id)
            cursor.close()
            return project
        }
        cursor.close()
        return valueIfNull
    }

    fun readStakeholder(project: Project?): List<Stakeholder> {
        val cursor = getCursor(StakeholderTableEntry.TABLE_NAME, arrayOf(StakeholderTableEntry.ID,StakeholderTableEntry.PROJECT_ID, StakeholderTableEntry.NAME, StakeholderTableEntry.DESCRIPTION), if (project != null) StakeholderTableEntry.PROJECT_ID + LIKE + QUOTES + project.id + QUOTES else ONE + EQ + ONE, null, null, null, null, null)
        val stakeholders = ArrayList<Stakeholder>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val projectId = cursor.getString(1)
                val name = cursor.getString(2)
                val description = cursor.getString(3)
                val stakeholder = Stakeholder.StakeholderBuilder(id, projectId, name, description).build()
                stakeholder.changeTimeMs = readVersioning(id)
                stakeholders.add(stakeholder)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return stakeholders
    }

    fun readStakeholder(key: String, valueIfNull: Stakeholder): Stakeholder {
        val cursor = getCursor(StakeholderTableEntry.TABLE_NAME, arrayOf(StakeholderTableEntry.ID, StakeholderTableEntry.PROJECT_ID, StakeholderTableEntry.NAME, StakeholderTableEntry.DESCRIPTION), StakeholderTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val projectId = cursor.getString(1)
            val name = cursor.getString(2)
            val description = cursor.getString(3)
            val stakeholder = Stakeholder.StakeholderBuilder(id, projectId, name, description).build()
            stakeholder.changeTimeMs = readVersioning(id)
            cursor.close()
            return stakeholder
        }
        cursor.close()
        return valueIfNull
    }

    fun readObject(key: String, valueIfNull: AbstractObject, fullLoad: Boolean = false): AbstractObject {
        val cursor = getCursor(ObjectTableEntry.TABLE_NAME, arrayOf(ObjectTableEntry.ID, ObjectTableEntry.SCENARIO_ID, ObjectTableEntry.NAME, ObjectTableEntry.DESCRIPTION, ObjectTableEntry.IS_RESOURCE), ObjectTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val scenarioId = cursor.getString(1)
            val name = cursor.getString(2)
            val description = cursor.getString(3)
            val isResource = cursor.getBoolean(4)
            val objectBuilder: AbstractObject.AbstractObjectBuilder
            if (isResource) {
                objectBuilder = Resource.ResourceBuilder(id, scenarioId, name, description).configure(
                        readInt(MIN_IDENTIFIER.plus(id), 0),
                        readInt(MAX_IDENTIFIER.plus(id), 0),
                        readInt(INIT_IDENTIFIER.plus(id), 0))
            } else {
                objectBuilder = ContextObject.ContextObjectBuilder(id, scenarioId, name, description)
            }
            if (fullLoad) {
                objectBuilder.addAttributes(attributes = *readAttributes(id).toTypedArray())
            }
            val abstractObject = objectBuilder.build()
            abstractObject.changeTimeMs = readVersioning(id)
            cursor.close()
            return abstractObject
        }
        cursor.close()
        return valueIfNull
    }

    fun readObjects(scenario: Scenario?, fullLoad: Boolean = false): List<AbstractObject> {
        val cursor = getCursor(ObjectTableEntry.TABLE_NAME, arrayOf(ObjectTableEntry.ID, ObjectTableEntry.SCENARIO_ID, ObjectTableEntry.NAME, ObjectTableEntry.DESCRIPTION, ObjectTableEntry.IS_RESOURCE), if (scenario != null) (ObjectTableEntry.SCENARIO_ID + LIKE + QUOTES + scenario.id + QUOTES) else (ONE + EQ + ONE), null, null, null, null, null)
        val objects = ArrayList<AbstractObject>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val scenarioId = cursor.getString(1)
                val name = cursor.getString(2)
                val description = cursor.getString(3)
                val isResource = cursor.getBoolean(4)
                val objectBuilder: AbstractObject.AbstractObjectBuilder
                if (isResource) {
                    objectBuilder = Resource.ResourceBuilder(id, scenarioId, name, description).configure(
                            readInt(MIN_IDENTIFIER.plus(id), 0),
                            readInt(MAX_IDENTIFIER.plus(id), 0),
                            readInt(INIT_IDENTIFIER.plus(id), 0)
                    )
                } else {
                    objectBuilder = ContextObject.ContextObjectBuilder(id, scenarioId, name, description)
                }
                if (fullLoad) {
                    objectBuilder.addAttributes(attributes = *readAttributes(id).toTypedArray())
                }
                val abstractObject = objectBuilder.build()
                abstractObject.changeTimeMs = readVersioning(id)
                objects.add(abstractObject)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return objects
    }

    fun readAttribute(key: String, valueIfNull: Attribute): Attribute {
        val cursor = getCursor(AttributeTableEntry.TABLE_NAME, arrayOf(AttributeTableEntry.ID, AttributeTableEntry.KEY, AttributeTableEntry.VALUE), AttributeTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val scenarioId = cursor.getString(1)
            val name = cursor.getString(2)
            val description = cursor.getString(3)
            val attribute = Attribute.AttributeBuilder(id, scenarioId, name, description).build()
            attribute.changeTimeMs = readVersioning(attribute.getVersioningId())
            cursor.close()
            return attribute
        }
        cursor.close()
        return valueIfNull
    }

    fun readAttributes(refId: String, type: String? = null): List<Attribute> {
        val cursor = getCursor(AttributeTableEntry.TABLE_NAME, arrayOf(AttributeTableEntry.ID, AttributeTableEntry.KEY, AttributeTableEntry.VALUE), (AttributeTableEntry.REF_ID + LIKE + QUOTES + refId + QUOTES) + if (type == null) NOTHING else (AND + AttributeTableEntry.TYPE + LIKE + QUOTES + type + QUOTES), null, null, null, null, null)
        val attributes = ArrayList<Attribute>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val key = cursor.getString(1)
                val value = cursor.getString(2)
                val attribute = Attribute.AttributeBuilder(id, refId, key, value).withAttributeType(type).build()
                attribute.changeTimeMs = readVersioning(attribute.getVersioningId())
                attributes.add(attribute)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return attributes
    }

    fun readScenario(key: String, valueIfNull: Scenario, fullLoad: Boolean = false): Scenario {
        val cursor = getCursor(ScenarioTableEntry.TABLE_NAME, arrayOf(ScenarioTableEntry.ID, ScenarioTableEntry.PROJECT_ID, ScenarioTableEntry.TITLE, ScenarioTableEntry.INTRO, ScenarioTableEntry.OUTRO), ScenarioTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val projectId = cursor.getString(1)
            val title = cursor.getString(2)
            val intro = cursor.getString(3)
            val outro = cursor.getString(4)
            val scenarioBuilder = Scenario.ScenarioBuilder(id, projectId, title, intro, outro)
            if (fullLoad) {
                scenarioBuilder.addObjects(obj = *readObjects(scenarioBuilder.build(), true).toTypedArray())
                scenarioBuilder.addPaths(path = *readPaths(scenarioBuilder.build(), true).toTypedArray())
            }
            val scenario = scenarioBuilder.build()
            scenario.changeTimeMs = readVersioning(id)
            cursor.close()
            return scenario
        }
        cursor.close()
        return valueIfNull
    }

    fun readScenarios(project: Project?, fullLoad: Boolean = false): List<Scenario> {
        val cursor = getCursor(ScenarioTableEntry.TABLE_NAME, arrayOf(ScenarioTableEntry.ID, ScenarioTableEntry.PROJECT_ID, ScenarioTableEntry.TITLE, ScenarioTableEntry.INTRO, ScenarioTableEntry.OUTRO), if (project != null) ScenarioTableEntry.PROJECT_ID + LIKE + QUOTES + project.id + QUOTES else ONE + EQ + ONE, null, null, null, null, null)
        val scenarios = ArrayList<Scenario>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val projectId = cursor.getString(1)
                val title = cursor.getString(2)
                val intro = cursor.getString(3)
                val outro = cursor.getString(4)
                val scenarioBuilder = Scenario.ScenarioBuilder(id, projectId, title, intro, outro)
                if (fullLoad) {
                    scenarioBuilder.addObjects(obj = *readObjects(scenarioBuilder.build(), true).toTypedArray())
                }
                val scenario = scenarioBuilder.build()
                scenario.changeTimeMs = readVersioning(id)
                scenarios.add(scenario)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return scenarios
    }

    fun readPaths(scenario: Scenario, fullLoad: Boolean = false): List<Path> {
        val cursor = getCursor(PathTableEntry.TABLE_NAME, arrayOf(PathTableEntry.ID, PathTableEntry.STAKEHOLDER_ID, PathTableEntry.LAYER), PathTableEntry.SCENARIO_ID + LIKE + QUOTES + scenario.id + QUOTES, null, null, null, null, null)
        val paths = ArrayList<Path>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val stakeholderId = cursor.getString(1)
                val layer = cursor.getInt(2)
                val path = Path.PathBuilder(id, scenario.id, readStakeholder(stakeholderId, NullHelper.get(Stakeholder::class)), layer).build()
                if (fullLoad) {
                    for (element in readElements(path, true)) {
                        path.add(element)
                    }
                }
                path.changeTimeMs = readVersioning(id)
                paths.add(path)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return paths
    }

    fun readPath(pathId: String, valueIfNull: Path, fullLoad: Boolean = false): Path {
        val cursor = getCursor(PathTableEntry.TABLE_NAME, arrayOf(PathTableEntry.SCENARIO_ID, PathTableEntry.STAKEHOLDER_ID, PathTableEntry.LAYER), PathTableEntry.ID + LIKE + QUOTES + pathId + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val stakeholderId = cursor.getString(1)
            val layer = cursor.getInt(2)
            val path = Path.PathBuilder(pathId, id, readStakeholder(stakeholderId, NullHelper.get(Stakeholder::class)), layer).build()
            if (fullLoad) {
                for (element in readElements(path, true)) {
                    path.add(element)
                }
            }
            path.changeTimeMs = readVersioning(id)
            cursor.close()
            return path
        }
        cursor.close()
        return valueIfNull
    }

    @Suppress("UNCHECKED_CAST")
    fun readElements(path: Path?, fullLoad: Boolean = false): List<IElement> {
        val cursor = getCursor(ElementTableEntry.TABLE_NAME, arrayOf(ElementTableEntry.ID, ElementTableEntry.PATH_ID, ElementTableEntry.PREV_ID, ElementTableEntry.TYPE, ElementTableEntry.TITLE, ElementTableEntry.TEXT), if (path != null) (ElementTableEntry.PATH_ID + LIKE + QUOTES + path.id + QUOTES) else (ONE + EQ + ONE), null, null, null, null, null)
        val elements = ArrayList<IElement>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val pathId = cursor.getString(1)
                val prevId = cursor.getString(2)
                val type = cursor.getString(3)
                val additionalInfo = cursor.getString(4)
                val text = cursor.getString(5)
                when (type) {
                    //STEPS
                    TYPE_STANDARD_STEP -> {
                        val step = StandardStep(id, prevId, pathId).withText(text).withTitle(additionalInfo)
                        if (fullLoad) {
                            for (linkAttribute in readAttributes(id, TYPE_OBJECT)) {
                                step.withObject(readObject(linkAttribute.key as String, NullHelper.get(AbstractObject::class)))
                            }
                        }
                        step.changeTimeMs = readVersioning(id)
                        readWhatIfs(id, step)
                        elements.add(step)
                    }
                    TYPE_JUMP_STEP -> {
                        val step = JumpStep(id, prevId, pathId).withTargetStep(readString(TARGET_STEP_UID_IDENTIFIER.plus(id), NOTHING)).withText(text).withTitle(additionalInfo)
                        if (fullLoad) {
                            for (linkAttribute in readAttributes(id, TYPE_OBJECT)) {
                                step.withObject(readObject(linkAttribute.key as String, NullHelper.get(AbstractObject::class)))
                            }
                        }
                        step.changeTimeMs = readVersioning(id)
                        readWhatIfs(id, step)
                        elements.add(step)
                    }
                    TYPE_SOUND_STEP -> {
                        val step = SoundStep(id, prevId, pathId).withText(text).withTitle(additionalInfo)
                        if (fullLoad) {
                            for (linkAttribute in readAttributes(id, TYPE_OBJECT)) {
                                step.withObject(readObject(linkAttribute.key as String, NullHelper.get(AbstractObject::class)))
                            }
                        }
                        step.changeTimeMs = readVersioning(id)
                        readWhatIfs(id, step)
                        elements.add(step)
                    }
                    TYPE_VIBRATION_STEP -> {
                        val step = VibrationStep(id, prevId, pathId).withText(text).withTitle(additionalInfo)
                        if (fullLoad) {
                            for (linkAttribute in readAttributes(id, TYPE_OBJECT)) {
                                step.withObject(readObject(linkAttribute.key as String, NullHelper.get(AbstractObject::class)))
                            }
                        }
                        step.changeTimeMs = readVersioning(id)
                        readWhatIfs(id, step)
                        elements.add(step)
                    }
                    TYPE_RESOURCE_STEP -> {
                        val attributes = readAttributes(id, TYPE_RESOURCE)
                        var resource: Resource? = null
                        if (attributes.size == 1) {
                            resource = readObject(attributes.first().key!!, NullHelper.get(Resource::class), true) as Resource
                        }
                        val step = ResourceStep(id, prevId, pathId).withChange(readInt(CHANGE_UID_IDENTIFIER.plus(id), Constants.ZERO)).withResource(resource).withText(text).withTitle(additionalInfo)
                        if (fullLoad) {
                            for (linkAttribute in readAttributes(id, TYPE_OBJECT)) {
                                step.withObject(readObject(linkAttribute.key as String, NullHelper.get(AbstractObject::class)))
                            }
                        }
                        step.changeTimeMs = readVersioning(id)
                        readWhatIfs(id, step)
                        elements.add(step)
                    }
                    //TRIGGERS
                    TYPE_BUTTON_TRIGGER -> {
                        val trigger = ButtonTrigger(id, prevId, pathId).withButtonLabel(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_IF_ELSE_TRIGGER -> {
                        val trigger = IfElseTrigger(id, prevId, pathId, text, additionalInfo)
                        var readByteArray = readByteArray(HASH_MAP_OPTIONS_IDENTIFIER.plus(id), byteArrayOf())
                        if (!readByteArray.isEmpty()) {
                            trigger.withPathOptions(
                                    ObjectHelper.nvl(
                                            DataHelper.toObject(readByteArray, HashMap::class), HashMap<Int, String>()) as HashMap<Int, String>)
                        }
                        readByteArray = readByteArray(HASH_MAP_LINK_IDENTIFIER.plus(id), byteArrayOf())
                        if (!readByteArray.isEmpty()) {
                            trigger.withOptionLayerLink(
                                    ObjectHelper.nvl(
                                            DataHelper.toObject(readByteArray, HashMap::class), HashMap<Int, Int>()) as HashMap<Int, Int>)
                        }
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_STAKEHOLDER_INTERACTION_TRIGGER -> {
                        val trigger = StakeholderInteractionTrigger(id, prevId, pathId).withText(text).withInteractedStakeholderId(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_INPUT_TRIGGER -> {
                        val trigger = InputTrigger(id, prevId, pathId).withText(text).withInput(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_RESOURCE_CHECK_TRIGGER -> {
                        val attributes = readAttributes(id, TYPE_RESOURCE)
                        var resource: Resource? = null
                        if (attributes.size == 1) {
                            resource = readObject(attributes.first().key!!, NullHelper.get(Resource::class), true) as Resource
                        }
                        val trigger = ResourceCheckTrigger(id, prevId, pathId).withButtonLabel(additionalInfo)
                                .withFalseStepId(text).withResource(resource).withCheckValue(readInt(CHECK_VALUE_UID_IDENTIFIER.plus(id), Constants.ZERO)).withMode(readString(CHECK_MODE_UID_IDENTIFIER.plus(id), NOTHING))
                        trigger.changeTimeMs = readVersioning(id)

                        elements.add(trigger)
                    }
                    TYPE_TIME_TRIGGER -> {
                        val trigger = TimeTrigger(id, prevId, pathId).withText(text).withTimeMillisecondSecond(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_SOUND_TRIGGER -> {
                        val trigger = SoundTrigger(id, prevId, pathId).withText(text).withDb(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_BLUETOOTH_TRIGGER -> {
                        val trigger = BluetoothTrigger(id, prevId, pathId).withText(text).withDeviceId(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_GPS_TRIGGER -> {
                        val trigger = GpsTrigger(id, prevId, pathId).withText(text).withGpsData(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_NFC_TRIGGER -> {
                        val trigger = NfcTrigger(id, prevId, pathId).withText(text).withMessage(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_WIFI_TRIGGER -> {
                        val trigger = WifiTrigger(id, prevId, pathId).withText(text).withSsidAndStrength(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_CALL_TRIGGER -> {
                        val trigger = CallTrigger(id, prevId, pathId).withText(text).withTelephoneNr(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_SMS_TRIGGER -> {
                        val trigger = SmsTrigger(id, prevId, pathId).withText(text).withTelephoneNr(additionalInfo)
                        trigger.changeTimeMs = readVersioning(id)
                        elements.add(trigger)
                    }
                    TYPE_ACCELERATION_TRIGGER -> {/*TODO*/
                    }
                    TYPE_GYROSCOPE_TRIGGER -> {/*TODO*/
                    }
                    TYPE_LIGHT_TRIGGER -> {/*TODO*/
                    }
                    TYPE_MAGNETOMETER_TRIGGER -> {/*TODO*/
                    }
                    else -> {
                        deleteElement(id)
                    }
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return elements
    }

    @Suppress("UNCHECKED_CAST")
    private fun readWhatIfs(id: String?, step: AbstractStep) {
        val readByteArray = readByteArray(ARRAY_LIST_WHAT_IF_IDENTIFIER.plus(id), byteArrayOf())
        if (!readByteArray.isEmpty()) {
            step.withWhatIfs(
                    ObjectHelper.nvl(
                            DataHelper.toObject(readByteArray, ArrayList::class), ArrayList<String>()) as ArrayList<String>)
        }
    }

    fun readWalkthrough(key: String, valueIfNull: Walkthrough): Walkthrough {
        val cursor = getCursor(WalkthroughTableEntry.TABLE_NAME, arrayOf(WalkthroughTableEntry.ID, WalkthroughTableEntry.SCENARIO_ID, WalkthroughTableEntry.OWNER, WalkthroughTableEntry.STAKEHOLDER_ID, WalkthroughTableEntry.XML_DATA), WalkthroughTableEntry.ID + LIKE + QUOTES + key + QUOTES, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            val id = cursor.getString(0)
            val owner = cursor.getString(1)
            val scenarioId = cursor.getString(2)
            val stakeholderId = cursor.getString(3)
            val xml = cursor.getString(4)
            val walkthrough = Walkthrough.WalkthroughBuilder(id, scenarioId, owner, readStakeholder(stakeholderId, Stakeholder.NullStakeholder(stakeholderId))).withXml(xml).build()
            walkthrough.changeTimeMs = readVersioning(id)
            cursor.close()
            return walkthrough
        }
        cursor.close()
        return valueIfNull
    }

    fun readWalkthroughs(key: String?): List<Walkthrough> {
        val cursor = getCursor(WalkthroughTableEntry.TABLE_NAME, arrayOf(WalkthroughTableEntry.ID, WalkthroughTableEntry.SCENARIO_ID, WalkthroughTableEntry.OWNER, WalkthroughTableEntry.STAKEHOLDER_ID, WalkthroughTableEntry.XML_DATA), WalkthroughTableEntry.SCENARIO_ID + LIKE + QUOTES + (key
                ?: ANY) + QUOTES, null, null, null, null, null)
        val walkthroughs = ArrayList<Walkthrough>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(0)
                val owner = cursor.getString(1)
                val scenarioId = cursor.getString(2)
                val stakeholderId = cursor.getString(3)
                val xml = cursor.getString(4)
                val walkthrough = Walkthrough.WalkthroughBuilder(id, scenarioId, owner, readStakeholder(stakeholderId, Stakeholder.NullStakeholder(stakeholderId))).withXml(xml).build()
                walkthrough.changeTimeMs = readVersioning(id)
                walkthroughs.add(walkthrough)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return walkthroughs
    }

    private fun addVersioning(id: String, oldTimeStamp: Long): Long {
        var time = oldTimeStamp
        if (disableNewVersion && oldTimeStamp != 0L) {
            writeLong(VERSIONING_IDENTIFIER.plus(id), oldTimeStamp)
        } else {
            writeLong(VERSIONING_IDENTIFIER.plus(id), System.currentTimeMillis())
            time = System.currentTimeMillis()
        }
        return time
    }

    private fun readVersioning(id: String): Long {
        return readLong(VERSIONING_IDENTIFIER.plus(id), 0)
    }

    private fun deleteVersioning(id: String) {
        deleteNumber(VERSIONING_IDENTIFIER.plus(id))
    }

    /** DELETE     **
     ** FROM     **
     ** DATABASE **/
    fun deleteNumber(key: String) {
        delete(NumberTableEntry.TABLE_NAME, NumberTableEntry.KEY, key)
    }

    fun deleteString(key: String) {
        delete(TextTableEntry.TABLE_NAME, TextTableEntry.KEY, key)
    }

    fun deleteData(key: String) {
        delete(DataTableEntry.TABLE_NAME, DataTableEntry.KEY, key)
    }

    fun deleteProject(key: String) {
        delete(ProjectTableEntry.TABLE_NAME, ProjectTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteStakeholder(key: String) {
        delete(StakeholderTableEntry.TABLE_NAME, StakeholderTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteObject(key: String) {
        delete(ObjectTableEntry.TABLE_NAME, ObjectTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteAttribute(key: String) {
        delete(AttributeTableEntry.TABLE_NAME, AttributeTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteAttributeByRefId(key: String) {
        delete(AttributeTableEntry.TABLE_NAME, AttributeTableEntry.REF_ID, key)
        deleteVersioning(key)
    }

    fun deleteAttributeByKey(key: String) {
        delete(AttributeTableEntry.TABLE_NAME, AttributeTableEntry.KEY, key)
        deleteVersioning(key)
    }

    fun deleteScenario(key: String) {
        delete(ScenarioTableEntry.TABLE_NAME, ScenarioTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deletePath(key: String) {
        delete(PathTableEntry.TABLE_NAME, PathTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteElement(key: String) {
        delete(ElementTableEntry.TABLE_NAME, ElementTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun deleteWalkthrough(key: String) {
        delete(WalkthroughTableEntry.TABLE_NAME, WalkthroughTableEntry.ID, key)
        deleteVersioning(key)
    }

    fun truncateStrings() {
        truncate(TextTableEntry.TABLE_NAME)
    }

    fun truncateNumbers() {
        truncate(NumberTableEntry.TABLE_NAME)
    }

    fun truncateData() {
        truncate(DataTableEntry.TABLE_NAME)
    }

    fun truncateProjects() {
        truncate(ProjectTableEntry.TABLE_NAME)
    }

    fun truncateStakeholders() {
        truncate(StakeholderTableEntry.TABLE_NAME)
    }

    fun truncateObjects() {
        truncate(ObjectTableEntry.TABLE_NAME)
    }

    fun truncateAttributes() {
        truncate(AttributeTableEntry.TABLE_NAME)
    }

    fun truncateScenarios() {
        truncate(ScenarioTableEntry.TABLE_NAME)
    }

    /** INTERNAL **/
    private fun insert(tableName: String, keyColumn: String, key: String, values: ContentValues): Long {
        delete(tableName, keyColumn, key)
        return getDb().insert(tableName, "null", values)
    }

    private fun delete(tableName: String, keyColumn: String, key: String) {
        val selection = keyColumn + LIKE_WILDCARD
        getDb().delete(tableName, selection, arrayOf(key))
    }

    private fun truncate(tableName: String) {
        getDb().delete(tableName, null, null)
    }

    public fun dropAndRecreateTable(table: String) {
        val collectTables = collectTables(table)
        if (!collectTables.isEmpty()) {
            openWritable()
            for (statement in collectTables) {
                getDb().execSQL(statement)
            }
            close()
        }
    }

    public fun reCreateIndices() {
        reCreateIndicesInternal(
                IndexCreationWrapper(TextTableEntry.TABLE_NAME,arrayOf(TextTableEntry.KEY)),
                IndexCreationWrapper(NumberTableEntry.TABLE_NAME,arrayOf(NumberTableEntry.KEY)),
                IndexCreationWrapper(DataTableEntry.TABLE_NAME,arrayOf(DataTableEntry.KEY)),
                IndexCreationWrapper(ProjectTableEntry.TABLE_NAME,arrayOf(ProjectTableEntry.ID)),
                IndexCreationWrapper(StakeholderTableEntry.TABLE_NAME,arrayOf(StakeholderTableEntry.PROJECT_ID)),
                IndexCreationWrapper(ScenarioTableEntry.TABLE_NAME,arrayOf(ScenarioTableEntry.ID)),
                IndexCreationWrapper(ScenarioTableEntry.TABLE_NAME,arrayOf(ScenarioTableEntry.PROJECT_ID)),
                IndexCreationWrapper(ObjectTableEntry.TABLE_NAME,arrayOf(ObjectTableEntry.ID)),
                IndexCreationWrapper(ObjectTableEntry.TABLE_NAME,arrayOf(ObjectTableEntry.SCENARIO_ID)),
                IndexCreationWrapper(AttributeTableEntry.TABLE_NAME,arrayOf(AttributeTableEntry.ID)),
                IndexCreationWrapper(AttributeTableEntry.TABLE_NAME,arrayOf(AttributeTableEntry.REF_ID)),
                IndexCreationWrapper(AttributeTableEntry.TABLE_NAME,arrayOf(AttributeTableEntry.REF_ID,AttributeTableEntry.TYPE)),
                IndexCreationWrapper(PathTableEntry.TABLE_NAME,arrayOf(PathTableEntry.ID)),
                IndexCreationWrapper(PathTableEntry.TABLE_NAME,arrayOf(PathTableEntry.SCENARIO_ID)),
                IndexCreationWrapper(ElementTableEntry.TABLE_NAME,arrayOf(ElementTableEntry.PATH_ID)),
                IndexCreationWrapper(WalkthroughTableEntry.TABLE_NAME,arrayOf(WalkthroughTableEntry.ID)),
                IndexCreationWrapper(WalkthroughTableEntry.TABLE_NAME,arrayOf(WalkthroughTableEntry.SCENARIO_ID))
        )
    }

    private fun reCreateIndicesInternal(vararg indexWrapper: IndexCreationWrapper){
        openWritable()
        try{
        for (wrapper in indexWrapper){
            getDb().execSQL(wrapper.getDeletionSql())
            getDb().execSQL(wrapper.getCreationSql())
            getDb().execSQL(wrapper.getStatisticsSql())
        }
        }catch (e: Exception){
            //NOP
        }
    }

    class IndexCreationWrapper(tableName: String, columnNames: Array<String>){

        private var tableNameProcessed = StringHelper.stripBlank(tableName).toUpperCase()
        private var columnNamesProcessed = Array<String>(columnNames.size) { columnIndex -> StringHelper.stripBlank(columnNames[columnIndex]).toUpperCase()}


        private fun getIndexName(): String {
            var indexName = tableNameProcessed
            for (column in columnNamesProcessed) {
                indexName += (GROUND_DASH + column)
            }
            return indexName
        }

        fun getDeletionSql(): String{
            if (columnNamesProcessed.isNotEmpty()) {
                return " DROP INDEX IF EXISTS ${getIndexName()} "
            }
            return NOTHING
        }

        fun getCreationSql(): String {
            if (columnNamesProcessed.isNotEmpty()) {
                return " CREATE INDEX IF NOT EXISTS ${getIndexName()} ON $tableNameProcessed (${StringHelper.concatTokens(COMMA,*columnNamesProcessed)}) "
            }
            return NOTHING
        }

        fun getStatisticsSql(): String {
            return " ANALYZE $tableNameProcessed "
        }
    }

}