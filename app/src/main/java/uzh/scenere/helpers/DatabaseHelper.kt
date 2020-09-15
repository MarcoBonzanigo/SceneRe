package uzh.scenere.helpers

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.ATTRIBUTE_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.ELEMENT_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.INIT_IDENTIFIER
import uzh.scenere.const.Constants.Companion.MAX_IDENTIFIER
import uzh.scenere.const.Constants.Companion.MIN_IDENTIFIER
import uzh.scenere.const.Constants.Companion.OBJECT_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.PATH_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.PROJECT_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.SCENARIO_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.STAKEHOLDER_UID_IDENTIFIER
import uzh.scenere.const.Constants.Companion.WALKTHROUGH_UID_IDENTIFIER
import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.database.SreDatabase
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.direct.IfElseTrigger
import uzh.scenere.views.Element
import java.io.Serializable
import kotlin.reflect.KClass

class DatabaseHelper private constructor(context: Context) {
    enum class DataMode {
        PREFERENCES, DATABASE
    }

    private var mode: DataMode = DataMode.PREFERENCES
    private var database: SreDatabase? = null
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE)

    fun disableNewVersioning(): DatabaseHelper{
        if (mode == DataMode.DATABASE && database != null){
            database?.disableNewVersion = true
        }
        return this
    }

    private fun enableNewVersioning(){
        if (mode == DataMode.DATABASE && database != null){
            database?.disableNewVersion = false
        }
    }

    init {
        if (PermissionHelper.check(context, PermissionHelper.Companion.PermissionGroups.STORAGE)) {
            database = SreDatabase.getInstance(context)
            mode = DataMode.DATABASE
        } else {
            mode = DataMode.PREFERENCES
        }
    }

    companion object {
        // Volatile: writes to this field are immediately made visible to other threads.
        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return when {
                instance != null -> {
                    if ((instance!!.mode == DataMode.PREFERENCES && PermissionHelper.check(context,PermissionHelper.Companion.PermissionGroups.STORAGE))){
                        instance = DatabaseHelper(context) // If permissions are granted, start over
                    }
                    instance!!
                }
                else -> synchronized(this) {
                    if (instance == null) {
                        instance = DatabaseHelper(context)
                    }
                    instance!!
                }
            }
        }
    }

    fun write(key: String, obj: Any, internalMode: DataMode = mode): Boolean {
        when (internalMode) {
            DataMode.PREFERENCES -> {
                if (obj is Boolean) sharedPreferences.edit().putBoolean(key, obj).apply()
                if (obj is String) sharedPreferences.edit().putString(key, obj).apply()
                if (obj is ByteArray) sharedPreferences.edit().putString(key, Base64.encodeToString(obj, Base64.DEFAULT)).apply()
                if (obj is Short) sharedPreferences.edit().putInt(key, obj.toInt()).apply()
                if (obj is Int) sharedPreferences.edit().putInt(key, obj).apply()
                if (obj is Long) sharedPreferences.edit().putLong(key, obj).apply()
                if (obj is Float) sharedPreferences.edit().putFloat(key, obj).apply()
                if (obj is Double) sharedPreferences.edit().putLong(key, java.lang.Double.doubleToLongBits(obj)).apply()
                if (obj is Project) write(PROJECT_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Stakeholder) write(STAKEHOLDER_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is AbstractObject) write(OBJECT_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Attribute) write(ATTRIBUTE_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Scenario) write(SCENARIO_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Path) write(PATH_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is IElement) write(ELEMENT_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
                if (obj is Walkthrough) write(WALKTHROUGH_UID_IDENTIFIER + key, DataHelper.toByteArray(obj))
            }
            DataMode.DATABASE -> {
                database!!.openWritable()
                if (obj is Boolean) database!!.writeBoolean(key, obj)
                if (obj is String) database!!.writeString(key, obj)
                if (obj is ByteArray) database!!.writeByteArray(key, obj)
                if (obj is Short) database!!.writeShort(key, obj)
                if (obj is Int) database!!.writeInt(key, obj)
                if (obj is Long) database!!.writeLong(key, obj)
                if (obj is Float) database!!.writeFloat(key, obj)
                if (obj is Double) database!!.writeDouble(key, obj)
                if (obj is Project) database!!.writeProject(obj)
                if (obj is Stakeholder) database!!.writeStakeholder(obj)
                if (obj is AbstractObject) database!!.writeObject(obj)
                if (obj is Attribute) database!!.writeAttribute(obj)
                if (obj is Scenario) database!!.writeScenario(obj)
                if (obj is Path) database!!.writePath(obj)
                if (obj is IElement) database!!.writeElement(obj)
                if (obj is Walkthrough) database!!.writeWalkthrough(obj)
                database!!.close()
            }
        }
        enableNewVersioning()
        return true
    }

    fun <T : Serializable> read(key: String, clz: KClass<T>, valIfNull: T = NullHelper.get(clz), internalMode: DataMode = mode): T {
        return when (internalMode) {
            DataMode.PREFERENCES -> readInternal(key,clz,valIfNull,internalMode)
            DataMode.DATABASE -> {
                database!!.openReadable()
                val readInternal = readInternal(key,clz,valIfNull,internalMode)
                database!!.close()
                readInternal
            }
        }
    }
    @Suppress("UNCHECKED_CAST")
    fun <T : Serializable> readInternal(key: String, clz: KClass<T>, valIfNull: T = NullHelper.get(clz), internalMode: DataMode = mode): T {
        try {
            when (internalMode) {
                DataMode.PREFERENCES -> {
                    if (String::class == clz) return sharedPreferences.getString(key, valIfNull as String) as T
                    if (ByteArray::class == clz) {
                        val byteArrayString: String = sharedPreferences.getString(key, "")
                        if (StringHelper.hasText(byteArrayString)) {
                            return Base64.decode(byteArrayString, Base64.DEFAULT) as T
                        }
                        return valIfNull
                    }
                    if (Boolean::class == clz) return sharedPreferences.getBoolean(key, valIfNull as Boolean) as T
                    if (Short::class == clz) {
                        val intValue = sharedPreferences.getInt(key, 0)
                        return if (intValue == 0) valIfNull else intValue.toShort() as T
                    }
                    if (Int::class == clz) return sharedPreferences.getInt(key, valIfNull as Int) as T
                    if (Long::class == clz) return sharedPreferences.getLong(key, valIfNull as Long) as T
                    if (Float::class == clz) return sharedPreferences.getFloat(key, valIfNull as Float) as T
                    if (Double::class == clz) {
                        val rawLongBits: Long = sharedPreferences.getLong(key, 0L)
                        return if (rawLongBits == 0L) valIfNull else java.lang.Double.longBitsToDouble(rawLongBits) as T
                    }
                    if (Project::class == clz) return readBinary(key, clz, PROJECT_UID_IDENTIFIER, valIfNull)
                    if (Stakeholder::class == clz) return readBinary(key, clz, STAKEHOLDER_UID_IDENTIFIER, valIfNull)
                    if (AbstractObject::class == clz) return readBinary(key, clz, OBJECT_UID_IDENTIFIER, valIfNull)
                    if (Attribute::class == clz) return readBinary(key, clz, ATTRIBUTE_UID_IDENTIFIER, valIfNull)
                    if (Scenario::class == clz) return readBinary(key, clz, SCENARIO_UID_IDENTIFIER, valIfNull)
                    if (Path::class == clz) return readBinary(key, clz, PATH_UID_IDENTIFIER, NullHelper.get(clz))
                    if (IElement::class == clz) return readBinary(key, clz, ELEMENT_UID_IDENTIFIER, NullHelper.get(clz))
                    if (Walkthrough::class == clz) return readBinary(key, clz, WALKTHROUGH_UID_IDENTIFIER, NullHelper.get(clz))
                }
                DataMode.DATABASE -> {
                    if (Boolean::class == clz) return database!!.readBoolean(key, valIfNull as Boolean) as T
                    if (String::class == clz) return database!!.readString(key, valIfNull as String) as T
                    if (ByteArray::class == clz) return database!!.readByteArray(key, valIfNull as ByteArray) as T
                    if (Short::class == clz) return database!!.readShort(key, valIfNull as Short) as T
                    if (Int::class == clz) return database!!.readInt(key, valIfNull as Int) as T
                    if (Long::class == clz) return database!!.readLong(key, valIfNull as Long) as T
                    if (Float::class == clz) return database!!.readFloat(key, valIfNull as Float) as T
                    if (Double::class == clz) return database!!.readDouble(key, valIfNull as Double) as T
                    if (Project::class == clz) return database!!.readProject(key, valIfNull as Project) as T
                    if (Stakeholder::class == clz) return database!!.readStakeholder(key, valIfNull as Stakeholder) as T
                    if (AbstractObject::class == clz) return database!!.readObject(key, valIfNull as AbstractObject) as T
                    if (Attribute::class == clz) return database!!.readAttribute(key, valIfNull as Attribute) as T
                    if (Scenario::class == clz) return database!!.readScenario(key, valIfNull as Scenario) as T
                    if (Path::class == clz) return database!!.readPath(key, valIfNull as Path) as T
                    if (IElement::class == clz) return valIfNull
                    if (Walkthrough::class == clz) return database!!.readWalkthrough(key, valIfNull as Walkthrough) as T
                }
            }
        } catch (e: Exception) {
        }
        return valIfNull
    }

    private fun <T : Serializable> readBinary(key: String, clz: KClass<T>, identifier: String, valIfNull: T): T {
        val bytes = read(identifier + key, ByteArray::class, ByteArray(0))
        if (bytes.isNotEmpty()) {
            val project = DataHelper.toObject(bytes, clz)
            return project ?: valIfNull
        }
        return valIfNull
    }

    fun <T : Serializable> readFull(key: String, clz: KClass<T>, internalMode: DataMode = mode): T? {
        return when (internalMode) {
            DataMode.PREFERENCES -> readFullInternal(key,clz,internalMode)
            DataMode.DATABASE -> {
                database!!.openReadable()
                val readFull = readFullInternal(key, clz, internalMode)
                database!!.close()
                readFull
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> readFullInternal(key: String, clz: KClass<T>, internalMode: DataMode = mode): T? {
        when (internalMode) {
            DataMode.PREFERENCES -> {
                if (Project::class == clz) return readBinary(key, clz, PROJECT_UID_IDENTIFIER, NullHelper.get(clz))
                if (AbstractObject::class == clz) return readBinary(key, clz, OBJECT_UID_IDENTIFIER, NullHelper.get(clz))
                if (Scenario::class == clz) return readBinary(key, clz, SCENARIO_UID_IDENTIFIER, NullHelper.get(clz))
                if (Path::class == clz) return readBinary(key, clz, PATH_UID_IDENTIFIER, NullHelper.get(clz))
                if (IElement::class == clz) return readBinary(key, clz, ELEMENT_UID_IDENTIFIER, NullHelper.get(clz))
                if (Walkthrough::class == clz) return readBinary(key, clz, WALKTHROUGH_UID_IDENTIFIER, NullHelper.get(clz))
            }
            DataMode.DATABASE -> {
                if (Project::class == clz) return database!!.readProject(key, NullHelper.get(Project::class), true) as T?
                if (AbstractObject::class == clz) return database!!.readObject(key, NullHelper.get(ContextObject::class), true) as T?
                if (Scenario::class == clz) return database!!.readScenario(key, NullHelper.get(Scenario::class), true) as T?
                if (Path::class == clz) return database!!.readPath(key, NullHelper.get(Path::class), true) as T?
                if (IElement::class == clz) return null
                if (Walkthrough::class == clz) return read(key, clz, NullHelper.get(clz))
            }
        }
        return null
    }

    fun <T : Serializable> readBulk(clz: KClass<T>, key: Any?, fullLoad: Boolean = false, internalMode: DataMode = mode): List<T> {
        return when (internalMode) {
            DataMode.PREFERENCES -> readBulkInternal(clz,key,fullLoad,internalMode)
            DataMode.DATABASE -> {
                database!!.openReadable()
                val readBulkInternal = readBulkInternal(clz, key, fullLoad, internalMode)
                database!!.close()
                readBulkInternal
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> readBulkInternal(clz: KClass<T>, key: Any?, fullLoad: Boolean = false, internalMode: DataMode = mode): List<T> {
        when (internalMode) {
            DataMode.PREFERENCES -> {
                if (Project::class == clz) return readBulkInternal(clz, PROJECT_UID_IDENTIFIER)
                if (Stakeholder::class == clz) {
                    val stakeholders = readBulkInternal(clz, STAKEHOLDER_UID_IDENTIFIER)
                    val list = ArrayList<Serializable>()
                    for (stakeholder in stakeholders) {
                        if ((stakeholder as Stakeholder).projectId == ((key as Project).id)) {
                            list.add(stakeholder)
                        }
                    }
                    return list as List<T>
                }
                if (AbstractObject::class == clz) {
                    val objects = readBulkInternal(clz, OBJECT_UID_IDENTIFIER)
                    val list = ArrayList<Serializable>()
                    for (obj in objects) {
                        if ((obj as AbstractObject).scenarioId == ((key as Scenario).id)) {
                            list.add(obj)
                        }
                    }
                    return list as List<T>
                }
                if (Attribute::class == clz) {
                    val attributes = readBulkInternal(clz, ATTRIBUTE_UID_IDENTIFIER)
                    val list = ArrayList<Serializable>()
                    for (attribute in attributes) {
                        if ((attribute as Attribute).refId == (key as String)) {
                            list.add(attribute)
                        }
                    }
                    return list as List<T>
                }
                if (Scenario::class == clz) {
                    val scenarios = readBulkInternal(clz, SCENARIO_UID_IDENTIFIER)
                    val list = ArrayList<Serializable>()
                    for (scenario in scenarios) {
                        if ((scenario as Scenario).projectId == ((key as Project).id)) {
                            list.add(scenario)
                        }
                    }
                    return list as List<T>
                }
                if (Path::class == clz) return emptyList()
                if (IElement::class == clz) return emptyList()
                if (Walkthrough::class == clz) {
                    val walkthroughs = readBulkInternal(clz, WALKTHROUGH_UID_IDENTIFIER)
                    val list = ArrayList<Serializable>()
                    for (walkthrough in walkthroughs) {
                        if ((walkthrough as Walkthrough).scenarioId == ((key as Scenario).id)) {
                            list.add(walkthrough)
                        }
                    }
                    return list as List<T>
                }
            }
            DataMode.DATABASE -> {
                if (Project::class == clz) return database!!.readProjects() as List<T>
                if (Stakeholder::class == clz && key is Project) return database!!.readStakeholder(key) as List<T>
                if (Stakeholder::class == clz && key == null) return database!!.readStakeholder(key) as List<T>
                if (AbstractObject::class == clz && key is Scenario) return database!!.readObjects(key, fullLoad) as List<T>
                if (AbstractObject::class == clz && key == null) return database!!.readObjects(key, fullLoad) as List<T>
                if (Attribute::class == clz && key is String) return database!!.readAttributes(key) as List<T>
                if (Scenario::class == clz && key is Project) return database!!.readScenarios(key, fullLoad) as List<T>
                if (Scenario::class == clz && key == null) return database!!.readScenarios(key, fullLoad) as List<T>
                if (Path::class == clz && key is Scenario) return database!!.readPaths(key, fullLoad) as List<T>
                if (IElement::class == clz && key is Path) return database!!.readElements(key, fullLoad) as List<T>
                if (IElement::class == clz && key == null) return database!!.readElements(key, fullLoad) as List<T>
                if (Walkthrough::class == clz && key is String?) return database!!.readWalkthroughs(key) as List<T>
                if (Walkthrough::class == clz && key is Scenario) return database!!.readWalkthroughs(key.id) as List<T>
            }
        }
        return emptyList()
    }

    fun <T : Serializable> readAndMigrate(key: String, clz: KClass<T>, valIfNull: T, deleteInPrefs: Boolean = true): T {
        when (mode) {
            DataMode.PREFERENCES -> {
                return read(key, clz, valIfNull)
            }
            DataMode.DATABASE -> {
                val readPref = read(key, clz, valIfNull, DataMode.PREFERENCES)
                database!!.openReadable()
                val readDb = read(key, clz, valIfNull, DataMode.DATABASE)
                database!!.close()
                if (readPref == valIfNull) {
                    //Val was not saved, return DB value
                    return readDb
                }
                //Delete from Prefs if not specified otherwise
                if (deleteInPrefs) {
                    delete(key, clz, DataMode.PREFERENCES)
                }
                //Value was saved, check DB value
                if (readDb == valIfNull) {
                    //DB value does'nt exist, write to DB, return
                    database!!.openWritable()
                    write(key, readPref)
                    database!!.close()
                    return readPref
                }
                //DB value did exist, return
                return readDb
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> readBulkInternal(clz: KClass<T>, identifier: String): List<T> {
        val list = ArrayList<Serializable>()
        for (entry in sharedPreferences.all) {
            if (entry.key.contains(identifier)) {
                val bytes = Base64.decode((entry.value as String), Base64.DEFAULT)
                val element = DataHelper.toObject(bytes, clz)
                if (element != null) {
                    list.add(element)
                }
            }
        }
        return list as List<T>
    }

    // Versioning
    fun isNewerVersion(versionItem: IVersionItem): Boolean {
       return when (versionItem){
            is Project -> versionItem.changeTimeMs > readVersioning(versionItem.id)
            is Stakeholder -> versionItem.changeTimeMs > readVersioning(versionItem.id)
            is Scenario -> versionItem.changeTimeMs > readVersioning(versionItem.id)
            is AbstractObject -> versionItem.changeTimeMs > readVersioning(versionItem.id)
            is Attribute -> versionItem.changeTimeMs > readVersioning(versionItem.getVersioningId())
            is Path -> versionItem.changeTimeMs > readVersioning(versionItem.id)
            is AbstractTrigger -> versionItem.changeTimeMs > readVersioning(versionItem.id)
            is AbstractStep -> versionItem.changeTimeMs > readVersioning(versionItem.id)
            is Walkthrough -> versionItem.changeTimeMs > readVersioning(versionItem.id)
            else -> false
        }
    }
    fun isNewerVersionBulk(versionItems: List<IVersionItem>): HashMap<KClass<*>,Int> {
        val map = HashMap<KClass<*>,Int>()
        for (item in versionItems){
            val new = when (item){
                is Project -> item.changeTimeMs > readVersioning(item.id,mode,false)
                is Stakeholder -> item.changeTimeMs > readVersioning(item.id,mode,false)
                is Scenario -> item.changeTimeMs > readVersioning(item.id,mode,false)
                is AbstractObject -> item.changeTimeMs > readVersioning(item.id,mode,false)
                is Attribute -> item.changeTimeMs > readVersioning(item.id,mode,false)
                is Path -> item.changeTimeMs > readVersioning(item.id,mode,false)
                is AbstractTrigger -> item.changeTimeMs > readVersioning(item.id,mode,false)
                is AbstractStep -> item.changeTimeMs > readVersioning(item.id,mode,false)
                is Walkthrough -> item.changeTimeMs > readVersioning(item.id,mode,false)
                else -> false
            }
            if (new){
                map.addOne(item::class)
            }
        }
        return map
    }

    fun addVersioning(id: String, internalMode: DataMode = mode){
        when (internalMode){
            DataMode.PREFERENCES -> sharedPreferences.edit().putLong(Constants.VERSIONING_IDENTIFIER.plus(id),System.currentTimeMillis()).apply()
            DataMode.DATABASE -> {
                database!!.openWritable()
                database?.writeLong(Constants.VERSIONING_IDENTIFIER.plus(id),System.currentTimeMillis())
                database!!.close()
            }
        }
    }

    fun readVersioning(id: String, internalMode: DataMode = mode, openDatabase: Boolean = true): Long{
        return when (internalMode){
            DataMode.PREFERENCES -> sharedPreferences.getLong(Constants.VERSIONING_IDENTIFIER.plus(id),0)
            DataMode.DATABASE -> {
                if (openDatabase){
                    database!!.openReadable()
                }
                val l = NumberHelper.nvl(database?.readLong(Constants.VERSIONING_IDENTIFIER.plus(id), 0), 0)
                if (openDatabase){
                    database!!.close()
                }
                l
            }
        }
    }

    fun deleteVersioning(id: String, internalMode: DataMode = mode){
        when (internalMode){
            DataMode.PREFERENCES -> sharedPreferences.edit().remove(Constants.VERSIONING_IDENTIFIER.plus(id)).apply()
            DataMode.DATABASE -> {
                database!!.openWritable()
                database?.deleteNumber(Constants.VERSIONING_IDENTIFIER.plus(id))
                database!!.close()
            }
        }
    }


    fun deletePreferenceUids(uidKey: String) {
        for (entry in sharedPreferences.all.entries) {
            if (entry.key.startsWith(uidKey)) {
                sharedPreferences.edit().remove(entry.key).apply()
            }
        }
    }

    fun <T : Serializable> delete(key: String, clz: KClass<T>, internalMode: DataMode = mode) {
        when (internalMode) {
            DataMode.PREFERENCES -> {
                sharedPreferences.edit().remove(key).apply()
                sharedPreferences.edit().remove(PROJECT_UID_IDENTIFIER + key).apply()
            }
            DataMode.DATABASE -> {
                database!!.openWritable()
                if (Project::class == clz) {
                    val project = readFull(key, Project::class)
                    if (project != null) {
                        for (stakeholder in project.stakeholders) {
                            delete(stakeholder.id, Stakeholder::class)
                        }
                        for (scenario in project.scenarios) {
                            delete(scenario.id, Scenario::class)
                        }
                    }
                    database!!.openWritable()
                    database!!.deleteProject(key)
                }
                if (Stakeholder::class == clz) {
                    database!!.deleteStakeholder(key)
                }
                if (AbstractObject::class == clz || ContextObject::class == clz || Resource::class == clz) {
                    val obj = readFull(key, AbstractObject::class)
                    database!!.openWritable()
                    database!!.deleteObject(key)
                    database!!.deleteAttributeByKey(key)
                    if (obj != null) {
                        for (attribute in obj.attributes) {
                            delete(attribute.id, Attribute::class)
                        }
                    }
                    if (obj is Resource) {
                        delete(MIN_IDENTIFIER.plus(obj.id), Double::class)
                        delete(MAX_IDENTIFIER.plus(obj.id), Double::class)
                        delete(INIT_IDENTIFIER.plus(obj.id), Double::class)
                    }
                }
                if (Attribute::class == clz) {
                    database!!.deleteAttribute(key)
                }
                if (Scenario::class == clz) {
                    val scenario = readFull(key, Scenario::class)
                    if (scenario != null) {
                        for (obj in scenario.objects) {
                            delete(obj.id, AbstractObject::class)
                        }
                        for (path in scenario.getAllPaths()) {
                            delete(path.id, Path::class)
                        }
                    }
                    database!!.openWritable()
                    database!!.deleteScenario(key)
                }
                if (Path::class == clz) {
                    val path = readFull(key, Path::class)
                    database!!.openWritable()
                    database!!.deletePath(key)
                    if (path != null) {
                        for (element in path.elements) {
                            if (element.value is IfElseTrigger) {
                                //Recursively delete
                                val scenario = readFull(path.scenarioId, Scenario::class)
                                database!!.openWritable()
                                for (entry in (element.value as IfElseTrigger).optionLayerLink.entries) {
                                    val p = scenario?.removePath(path.stakeholder, entry.value)
                                    if (p != null) {
                                        delete(p.id, Path::class)
                                    }
                                }
                            }
                            delete(element.value.getElementId(), IElement::class)
                        }
                    }
                }
                if (IElement::class == clz) {
                    database!!.deleteElement(key)
                    database!!.deleteAttributeByRefId(key)
                }
                if (Walkthrough::class == clz) {
                    database!!.deleteWalkthrough(key)
                }
                if (CollectionHelper.oneOf(clz, Boolean::class, Short::class, Int::class, Long::class, Float::class, Double::class)) {
                    database!!.deleteNumber(key)
                }
                if (String::class == clz) {
                    database!!.deleteString(key)
                }
                if (ByteArray::class == clz) {
                    database!!.deleteData(key)
                }
                database!!.close()
            }
        }
    }

    fun recreateTableStatistics(){
        if (mode == DataMode.DATABASE){
            database!!.reCreateIndices()
        }
    }

    fun clear() {
        when (mode) {
            DataMode.PREFERENCES -> {
                sharedPreferences.edit().clear().apply()
            }
            DataMode.DATABASE -> {
                database!!.openWritable()
                database!!.truncateData()
                database!!.truncateNumbers()
                database!!.truncateStrings()
                database!!.truncateProjects()
                database!!.truncateStakeholders()
                database!!.truncateObjects()
                database!!.truncateAttributes()
                database!!.truncateScenarios()
                database!!.close()
            }
        }
    }

    fun <T : Serializable> dropAndRecreate(kClass: KClass<T>) {
        database!!.openWritable()
        if (kClass == Attribute::class) {
            database!!.dropAndRecreateTable("ATTRIBUTE_TABLE")
        }
        if (kClass == Path::class) {
            database!!.dropAndRecreateTable("PATH_TABLE")
        }
        if (kClass == Element::class) {
            database!!.dropAndRecreateTable("ELEMENT_TABLE")
        }
        if (kClass == AbstractObject::class) {
            database!!.dropAndRecreateTable("OBJECT_TABLE")
        }
        database!!.close()
    }

    fun dropAndRecreateAll(){
        if (mode == DataMode.DATABASE) {
            database!!.openWritable()
            database!!.dropAndRecreateTable("ATTRIBUTE_TABLE")
            database!!.dropAndRecreateTable("PATH_TABLE")
            database!!.dropAndRecreateTable("ELEMENT_TABLE")
            database!!.dropAndRecreateTable("OBJECT_TABLE")
            database!!.dropAndRecreateTable("PROJECT_TABLE")
            database!!.dropAndRecreateTable("SCENARIO_TABLE")
            database!!.dropAndRecreateTable("STAKEHOLDER_TABLE")
            database!!.dropAndRecreateTable("WALKTHROUGH_TABLE")
            database!!.dropAndRecreateTable("NUMBER_TABLE")
            database!!.dropAndRecreateTable("DATA_TABLE")
            database!!.dropAndRecreateTable("TEXT_TABLE")
            database!!.close()
        }
        for (pref in sharedPreferences.all){
            sharedPreferences.edit().remove(pref.key).apply()
        }
    }

    fun dropAndRecreateWalkthroughs(){
        if (mode == DataMode.DATABASE) {
            database!!.openWritable()
            val walkthroughs = database!!.readWalkthroughs(null)
            for (walkthrough in walkthroughs){
                delete(walkthrough.id,Walkthrough::class)
            }
            database!!.close()
        }
    }


}