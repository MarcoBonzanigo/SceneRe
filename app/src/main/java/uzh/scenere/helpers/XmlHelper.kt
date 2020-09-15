package uzh.scenere.helpers

import android.content.Context

class XmlHelper(private var context: Context) {/**
    private var xml = context.resources.getString(R.string.xml_declaration).plus(NEW_LINE_C)
    private val nodes = ArrayList<String>()

    private fun <T : Serializable> withAttribute(value: T, key: String): XmlHelper {
        val valueClass: String = value::class.toString()
        nodes.add(context.getString(R.string.xml_enclosing, key, valueClass, value.toString()))
        return this
    }

    /**
     *
     *     <pathNrs type="List">
     *          <pathNr type="Int">1234</id>
     *          <pathNr type="Int">2345</id>
     *          <pathNr type="Int">3456</id>
     *     </pathNrs>
     *
     *     <paths type="List">
     *         <path type="Path">
     *              <id type="String">aaaa</id>
     *              <scenarioId type="String">bbbb</scenarioId>
     *              <stakeholderId type="String">cccc</stakeholderId>
     *              <layer type="Int">0</layer>
     *         </path>
     *         <path type="List">
     *              <id type="String">aaaa</id>
     *              <scenarioId type="String">bbbb</scenarioId>
     *              <stakeholderId type="String">cccc</stakeholderId>
     *              <layer type="Int">0</layer>
     *         </path>
     *     </paths>
     *
     */
    fun <T : Serializable> createComplex(value: T, key: String): String {
        var node = context.getString(R.string.xml_begin_tag, key, value.className()).plus(NEW_LINE_C)
        when (value) {
            is Path -> {
                node += context.getString(R.string.xml_enclosing, Path.id_, STRING, value.id).plus(NEW_LINE_C)
                node += context.getString(R.string.xml_enclosing, Path.scenarioId_, STRING, value.scenarioId).plus(NEW_LINE_C)
                node += context.getString(R.string.xml_enclosing, Path.layer_, INT, value.layer.toString()).plus(NEW_LINE_C)
                node += createComplex(value.stakeholder,Stakeholder.name_)
            }
            is Stakeholder -> {
                node += context.getString(R.string.xml_enclosing, Stakeholder.id_, STRING, value.id).plus(NEW_LINE_C)
                node += context.getString(R.string.xml_enclosing, Stakeholder.projectId_, STRING, value.projectId).plus(NEW_LINE_C)
                node += context.getString(R.string.xml_enclosing, Stakeholder.name_, STRING, value.name).plus(NEW_LINE_C)
                node += context.getString(R.string.xml_enclosing, Stakeholder.description_, STRING, value.description).plus(NEW_LINE_C)
            }
            is HashMap<*,*> -> {
                for (entry in value.entries){
                    node += if (entry.value is HashMap<*,*>){
                        createComplex(entry.value as HashMap<*,*>, entry.key.toString())
                    }else{
                        context.getString(R.string.xml_enclosing, entry.key, entry.value.className(), entry.value).plus(NEW_LINE_C)
                    }
                }
            }
            else ->{

            }
        }
        node += context.getString(R.string.xml_end_tag, key).plus(NEW_LINE_C)
        return node
    }

    fun <T : Serializable> withList(values: List<T>, key: String): XmlHelper {
        val listClazz = if (values.isEmpty()) EMPTY_LIST else values[0].className()
        var node = context.getString(R.string.xml_begin_tag, key, listClazz).plus(NEW_LINE_C)
        for (value in values) {
            node += when (value) {
                is Path -> createComplex(value, Path.name__)
                is Stakeholder -> createComplex(value, Stakeholder.name__)
                else -> context.getString(R.string.xml_enclosing, key, value.className(), value.toString()).plus(NEW_LINE_C)
            }
        }
        node += context.getString(R.string.xml_end_tag, key.plus(NEW_LINE_C))
        nodes.add(node)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun toXml(dataSheet: AbstractXmlDataSheet) {
        for (property in dataSheet.getProperties()) {
            val serializable = dataSheet.getProperty(property)
            if (serializable is List<*>) {
                withList(serializable as List<Serializable>, property)
            } else if (serializable != null) {
                withAttribute(serializable, property)
            }
        }
    }

    fun build(): String {
        for (node in nodes) {
            xml += node.plus(NEW_LINE_C)
        }
        return xml.substring(0, xml.length - (NEW_LINE_C.length + 1))
    }

    public abstract class AbstractXmlDataSheet {
        protected var propertiesMap: HashMap<String, Serializable> = HashMap()
        abstract fun getProperties(): Array<String>
        fun getProperty(key: String): Serializable? {
            return propertiesMap[key]
        }
    }
    */
}