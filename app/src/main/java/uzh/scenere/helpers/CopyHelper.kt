package uzh.scenere.helpers

import android.util.Log
import uzh.scenere.datamodel.Scenario
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CopyHelper {

    companion object {
        private val mapping = HashMap<String, String>()
        private val uuidPattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".toRegex()

        fun copy(scenario: Scenario, vararg uuidToIgnore: String): Scenario? {
            mapping.clear()
            for (uuid in uuidToIgnore) {
                mapping[uuid] = uuid
            }
            val newScenario = scenario
            try{
                analyze(newScenario)
            }catch(e: Exception){
                return null
            }
            return newScenario
        }

        @Suppress("UNCHECKED_CAST")
        fun analyze(obj: Any?): Any? {
            if (obj != null) {
                if (obj is String && obj.matches(uuidPattern)) {
                    return checkForUuidPattern(obj)
                } else if (obj is HashMap<*, *>) {
                    return handleHashMap(obj as HashMap<out Any, out Any>)
                } else if (obj is ArrayList<*>) {
                    return handleArrayList(obj as ArrayList<out Any>)
                } else if (obj is Array<*>) {
                    return handleArray(obj as Array<out Any>)
                }
                for (field in obj.getAllDeclaredFields()) {
                    if (field.toString().contains("java.lang.Class")) {
                        continue
                    }
                    field.isAccessible = true
                    if (field.get(obj) is String && (field.get(obj) as String).matches(uuidPattern)) {
                        val newUuid = checkForUuidPattern(field.get(obj) as String)
                        field.set(obj, newUuid)
                    } else if (field.get(obj) is HashMap<*, *>) {
                        val hashMap = handleHashMap(field.get(obj) as HashMap<out Any, out Any>)
                        field.set(obj, hashMap)
                    } else if (field.get(obj) is ArrayList<*>) {
                        val arrayList = handleArrayList(field.get(obj) as ArrayList<out Any>)
                        field.set(obj, arrayList)
                    } else if (field.get(obj) is Array<*> && (field.get(obj) as Array<out Any>).size > 0) {
                        val array1 = field.get(obj) as Array<out Any>
                        val array = handleArray(array1)
                        field.set(obj, array)
                    }
                    field.isAccessible = false
                }
            }
            return obj
        }

        private fun handleHashMap(map: HashMap<out Any, out Any>): HashMap<Any?, Any?> {
            val hashMap = HashMap<Any?, Any?>()
            for (item in map) {
                val obj1 = analyze(item.key)
                val obj2 = analyze(item.value)
                hashMap[obj1] = obj2
            }
            return hashMap
        }

        private fun handleArrayList(list: ArrayList<out Any>): ArrayList<Any?> {
            val arrayList = ArrayList<Any?>()
            for (item in list) {
                val newItem = analyze(item)
                arrayList.add(newItem)
            }
            return arrayList
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T: Any?> handleArray(array: Array<T>): Array<T> {
            val arrayCopy = array
            for (item in array.indices) {
                val newItem = analyze(array[item]) as T
                arrayCopy[item] = newItem
            }
            return arrayCopy
        }

        private fun checkForUuidPattern(obj: String): String? {
            if (mapping.containsKey(obj)) {
                val newUuid = mapping[obj]
                return newUuid
            } else {
                val newUuid = UUID.randomUUID().toString()
                mapping[obj] = newUuid
                mapping[newUuid] = newUuid
                return newUuid
            }
        }
    }
}