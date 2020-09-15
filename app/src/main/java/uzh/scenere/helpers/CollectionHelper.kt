package uzh.scenere.helpers

import uzh.scenere.datamodel.*
import uzh.scenere.datamodel.steps.AbstractStep
import uzh.scenere.datamodel.triggers.AbstractTrigger
import java.io.Serializable
import java.lang.reflect.Array

class CollectionHelper private constructor() {
    companion object { //Static Reference
        fun <T : Any> oneOf(value: T?, vararg values: T): Boolean {
            if (value == null || values.isEmpty()) {
                return false
            }
            for (v in values) {
                if (value == v) {
                    return true
                }
            }
            return false
        }


        fun containsOneOf(value: String, vararg values: String): Boolean {
            if (values.isEmpty()) {
                return false
            }
            for (v in values) {
                if (value.contains(v)) {
                    return true
                }
            }
            return false
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> subArray(clazz: Class<T>, source: List<T>, begin: Int, end: Int): kotlin.Array<T> {
            if (source.size < end || end <= begin) {
                return Array.newInstance(clazz, 0) as kotlin.Array<T>
            }
            val array: kotlin.Array<T> = Array.newInstance(clazz, end - begin) as kotlin.Array<T>
            for (i in begin until end) {
                array[i] = source[i]
            }
            return array
        }

        fun toIdStringList(serializables: List<out Serializable>): ArrayList<String> {
            val list = ArrayList<String>()
            for (serializable in serializables){
                when (serializable){
                    is Stakeholder -> list.add(serializable.id)
                    is Scenario -> list.add(serializable.id)
                    is Project -> list.add(serializable.id)
                    is AbstractObject -> list.add(serializable.id)
                    is Attribute -> list.add(serializable.id)
                    is Path -> list.add(serializable.id)
                    is AbstractStep -> list.add(serializable.id)
                    is AbstractTrigger -> list.add(serializable.id)
                }
            }
            return list
        }
    }
}