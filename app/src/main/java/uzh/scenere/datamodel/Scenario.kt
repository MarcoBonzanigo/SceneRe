package uzh.scenere.datamodel

import android.content.Context
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.steps.ResourceStep
import uzh.scenere.datamodel.triggers.direct.IfElseTrigger
import uzh.scenere.helpers.DatabaseHelper
import uzh.scenere.helpers.NumberHelper
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class Scenario private constructor(val id: String, val projectId: String, val title: String, val intro: String, val outro: String): Serializable, IVersionItem {
    override var changeTimeMs: Long = 0

    val objects: ArrayList<AbstractObject> = ArrayList()
    var paths: HashMap<String,HashMap<Int,Path>> = HashMap()

    fun getPath(stakeholder: Stakeholder, context: Context, layer: Int = -1 ): Path {
        val stakeholderPaths = paths[stakeholder.id]
        return if (stakeholderPaths == null){
            createPath(stakeholder,context)
        }else{
            //no layer specified or path is null, create one, otherwise fetch path
            var path: Path? = stakeholderPaths[layer]
            if (path == null || layer == -1){
                path = createPath(stakeholder,context)
            }
            path
        }
    }

    private fun createPath(stakeholder: Stakeholder, context: Context): Path{
        val stakeholderPaths = paths[stakeholder.id]
        if (stakeholderPaths != null){
            var maxLayer = 0
            for (entry in stakeholderPaths.entries){
                maxLayer = if (entry.key > maxLayer) entry.key else maxLayer
            }
            //create path with increased layer
            val path = Path.PathBuilder(id, stakeholder, maxLayer+1).build()
            DatabaseHelper.getInstance(context).write(path.id,path)
            stakeholderPaths[path.layer] = path
            paths[stakeholder.id] = stakeholderPaths
            return path
        }else{
            //create path map because it does not exist
            val path = Path.PathBuilder(id, stakeholder, 0).build()
            DatabaseHelper.getInstance(context).write(path.id,path)
            val pathMap = hashMapOf(0 to path)
            paths[stakeholder.id] = pathMap
            return path
        }
    }

    fun getAllPaths(stakeholder: Stakeholder): HashMap<Int, Path>? {
        return paths[stakeholder.id]
    }

    fun getAllPaths(): List<Path> {
        val pathList = ArrayList<Path>()
        for (entry in paths.entries){
            for (pathEntry in entry.value.entries){
                pathList.add(pathEntry.value)
            }
        }
        return pathList
    }

    fun updatePath(stakeholder: Stakeholder, path: Path ){
        if (paths[stakeholder.id] != null){
            paths[stakeholder.id]!![path.layer] = path
        }else{
            paths[stakeholder.id] = hashMapOf(path.layer to path)
        }
    }

    fun removePath(stakeholder: Stakeholder, pathLayer: Int?): Path?{
        if (pathLayer != null && paths.containsKey(stakeholder.id)) {
            val path = paths[stakeholder.id]!![pathLayer]
            paths[stakeholder.id]!!.remove(pathLayer)
            if (path != null){
                for (element in path.elements.entries){
                    if (element.value is IfElseTrigger){
                        //Recursively delete
                        for (entry in (element.value as IfElseTrigger).optionLayerLink.entries){
                            removePath(path.stakeholder,entry.value)
                        }
                    }
                }
            }
            return path
        }
        return null
    }

    fun getAttributesToObject(obj: AbstractObject): List<Attribute> {
        for (o in objects){
            if (o.id == obj.id){
                return o.attributes
            }
        }
        return emptyList()
    }

    fun getObjectNames(vararg additionalName: String): Array<String>{
        val list = ArrayList<String>()
        list.addAll(additionalName)
        for (obj in objects){
            list.add(obj.name)
        }
        return list.toTypedArray()
    }

    fun getObjectByName(name: String?): AbstractObject?{
        for (obj in objects){
            if (obj.name == name){
                return obj
            }
        }
        return null
    }

    fun hasStakeholderPath(stakeholder: Stakeholder): Boolean{
        val pathMap = paths[stakeholder.id]
        if (!pathMap.isNullOrEmpty() && NumberHelper.nvl(pathMap[0]?.elements?.size,0)>0){
            return true
        }
        return false
    }

    fun getObjectsWithNames(objectNames: ArrayList<String>): ArrayList<AbstractObject>{
        val objectList = ArrayList<AbstractObject>()
        for (name in objectNames){
            for (obj in this.objects){
                if (obj.name == name){
                    objectList.add(obj)
                }
            }
        }
        return objectList
    }

    fun getAllStepTitlesAndIds(stakeholder: Stakeholder, exceptId: String? = null): Pair<ArrayList<String>, ArrayList<String>> {
        val stepTitles = ArrayList<String>()
        val stepIds = ArrayList<String>()
        val allPaths = getAllPaths(stakeholder)
        if (allPaths != null) {
            for (path in allPaths) {
                for (entry in path.value.elements){
                    val element = entry.value
                    if (element is AbstractStep && element.title != null && element.id != exceptId) {
                        stepTitles.add(element.title!!)
                        stepIds.add(element.id)
                    }
                }
            }
        }
        return Pair(stepTitles, stepIds)
    }

    fun getAllStepTitles(): ArrayList<String> {
        val stepTitles = ArrayList<String>()
        val allPaths = getAllPaths()
        for (path in allPaths) {
            for (entry in path.elements){
                val element = entry.value
                if (element is AbstractStep && element.title != null) {
                    stepTitles.add(element.title!!)
                }
            }
        }
        return stepTitles
    }

    fun getAllUsedResources(): ArrayList<Resource> {
        val resources = ArrayList<Resource>()
        val allPaths = getAllPaths()
        for (path in allPaths) {
            for (entry in path.elements){
                val element = entry.value
                if (element is ResourceStep && element.resource != null) {
                    resources.add(element.resource!!)
                }
            }
        }
        return resources
    }

    fun getAllResources(): ArrayList<Resource> {
        val list = ArrayList<Resource>()
        for (obj in objects){
            if (obj.isResource){
                list.add(obj as Resource)
            }
        }
        return list
    }

    fun getAllStakeholdersWithPaths(context: Context): ArrayList<Stakeholder> {
        val list = ArrayList<Stakeholder>()
        for (entry in paths){
            val stakeholder = DatabaseHelper.getInstance(context).read(entry.key, Stakeholder::class)
            if (stakeholder !is Stakeholder.NullStakeholder){
                list.add(stakeholder)
            }
        }
        return list
    }

    fun getAllContextObject(): ArrayList<ContextObject> {
        val list = ArrayList<ContextObject>()
        for (obj in objects){
            if (!obj.isResource){
                list.add(obj as ContextObject)
            }
        }
        return list
    }

    fun getPathAndStepToStepId(stakeholderId: Stakeholder, id: String): Pair<AbstractStep?, Path?>  {
        var step: AbstractStep? = null
        var path: Path? = null
        val allPaths = getAllPaths(stakeholderId)
        if (allPaths != null) {
            for (p in allPaths) {
                for (entry in p.value.elements){
                    val element = entry.value
                    if (element is AbstractStep && element.id == id) {
                        step = element
                        path = p.value
                    }
                }
            }
        }
        return Pair(step,path)
    }

    class ScenarioBuilder(private val projectId: String, private val title: String, private val intro: String, private val outro: String){

        constructor(project: Project, title: String, intro: String, outro: String): this(project.id,title,intro,outro)

        constructor(id: String, project: Project, title: String, intro: String, outro: String): this(project,title,intro,outro){
            this.id = id
        }

        constructor(id: String, projectId: String, title: String, intro: String, outro: String): this(projectId,title,intro,outro){
            this.id = id
        }

        private var id: String? = null
        private var objects: List<AbstractObject> = ArrayList()
        private val paths: HashMap<String,HashMap<Int,Path>> = HashMap()

        fun addObjects(vararg obj: AbstractObject): ScenarioBuilder{
            this.objects = this.objects.plus(obj)
            return this
        }

        fun addPaths(vararg path: Path): ScenarioBuilder{
            for (p in path){
                if (paths[p.stakeholder.id] == null){
                    this.paths[p.stakeholder.id] = hashMapOf(p.layer to p)
                }else{
                    this.paths[p.stakeholder.id]!![p.layer] = p
                }
            }
            return this
        }

        fun copyId(scenario: Scenario) {
            this.id = scenario.id
        }

        fun build(): Scenario{
            val scenario  = Scenario(id?: UUID.randomUUID().toString(),projectId, title, intro, outro)
            scenario.objects.addAll(this.objects)
            scenario.paths = this.paths
            return scenario
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Scenario

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    class NullScenario(): Scenario("","","","","") {}
}