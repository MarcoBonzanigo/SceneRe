package uzh.scenere.views

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import uzh.scenere.const.Constants.Companion.BREAK
import uzh.scenere.const.Constants.Companion.MATERIAL_100_BLUE
import uzh.scenere.const.Constants.Companion.MATERIAL_100_GREEN
import uzh.scenere.const.Constants.Companion.MATERIAL_100_LIME
import uzh.scenere.const.Constants.Companion.MATERIAL_100_ORANGE
import uzh.scenere.const.Constants.Companion.MATERIAL_100_RED
import uzh.scenere.const.Constants.Companion.MATERIAL_100_TURQUOISE
import uzh.scenere.const.Constants.Companion.MATERIAL_100_VIOLET
import uzh.scenere.const.Constants.Companion.MATERIAL_100_YELLOW
import uzh.scenere.const.Constants.Companion.MATERIAL_700_BLUE
import uzh.scenere.const.Constants.Companion.MATERIAL_700_GREEN
import uzh.scenere.const.Constants.Companion.MATERIAL_700_LIME
import uzh.scenere.const.Constants.Companion.MATERIAL_700_ORANGE
import uzh.scenere.const.Constants.Companion.MATERIAL_700_RED
import uzh.scenere.const.Constants.Companion.MATERIAL_700_TURQUOISE
import uzh.scenere.const.Constants.Companion.MATERIAL_700_VIOLET
import uzh.scenere.const.Constants.Companion.MATERIAL_700_YELLOW
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.datamodel.Attribute
import uzh.scenere.datamodel.AbstractObject
import uzh.scenere.helpers.StringHelper
import java.io.Serializable
import kotlin.reflect.KClass


@SuppressLint("ViewConstructor")
class SreContextAwareTextView(context: Context, parent: ViewGroup?,var boldWords: ArrayList<String>, val objects: ArrayList<out Serializable>) : SreTextView(context,parent), Serializable {
    private val colorArray = if (style == TextStyle.DARK)
        arrayOf(MATERIAL_100_RED, MATERIAL_100_VIOLET, MATERIAL_100_BLUE, MATERIAL_100_TURQUOISE, MATERIAL_100_GREEN, MATERIAL_100_LIME, MATERIAL_100_YELLOW, MATERIAL_100_ORANGE) else
        arrayOf(MATERIAL_700_RED, MATERIAL_700_VIOLET, MATERIAL_700_BLUE, MATERIAL_700_TURQUOISE, MATERIAL_700_GREEN, MATERIAL_700_LIME, MATERIAL_700_YELLOW, MATERIAL_700_ORANGE)
    private var objectPointer = 0
    private val objectMap = HashMap<String, Serializable>()
    private val objectLabels = HashMap<String, String>()
    private val placeholder = "XXX"
    private val fontBegin = "<font color='XXX'>"
    private val fontEnd = "</font>"

    fun getObjectLabels(): ArrayList<String>{
        val list = ArrayList<String>()
        for (entry in objectLabels.entries){
            list.add(entry.key)
        }
        return list
    }

    init {
        for (s in boldWords.indices){
            boldWords[s] = boldWords[s].replace(BREAK,NEW_LINE)
        }
        if (!objects.isEmpty()) {
            addObjects(objects)
        }
        initHighlighting()
    }

    private fun initHighlighting() {
        addTextChangedListener(SreContentAwareTextWatcher(this))
    }

    fun setTextWithNewBoldWords(text: String, vararg newBoldWords: String){
        val list = ArrayList<String>()
        for (word in newBoldWords){
            if (!boldWords.contains(word)){
                list.add(word)
            }
        }
        list.addAll(boldWords)
        boldWords = list
        setText(text)
    }

    fun <T: Serializable>addObjects(objects: ArrayList<T>): SreContextAwareTextView {
        if (objectPointer >= colorArray.size){
            Log.e("AutoComplete","Not enough Colors defined.")
            return this
        }
        when (objects[0]) {
            is AbstractObject -> {
                for (obj in objects) {
                    val name = (obj as AbstractObject).name
                    objectMap[name] = obj
                    objectLabels[name] = fontBegin.replace(placeholder, colorArray[objectPointer]) + name + fontEnd
                }
            }
            is Attribute -> {
                for (obj in objects) {
                    val key = (obj as Attribute).key
                    if (key != null) {
                        objectMap[key] = obj
                        objectLabels[key] = fontBegin.replace(placeholder, colorArray[objectPointer]) + key + fontEnd
                    }
                }
            }

            is String -> {
                for (obj in objects) {
                    objectMap[(obj as String)] = obj
                    objectLabels[obj] = fontBegin.replace(placeholder, colorArray[objectPointer]) + obj + fontEnd
                }
            }
            else -> throw ClassNotFoundException("No Mapping for this Class available")
        }
        objectPointer++
        return this
    }

    fun <T: Serializable> getContextObjects(classFilter: KClass<T>? = null): ArrayList<Serializable> {
        val list = ArrayList<Serializable>()
        for (entry in objectMap){
            if (classFilter != null && entry.value::class != classFilter){
                continue
            }
            if (text.toString().contains(entry.key)){
                list.add(entry.value)
            }
        }
        return list
    }

    class SreContentAwareTextWatcher(private val textView: SreContextAwareTextView) : TextWatcher {
        private var ignore = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            //NOP
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (!ignore) {
                var str = s.toString()
                for (word in textView.boldWords) {
                    str = str.replaceFirst(word,"<b>$word</b>")
                }
                for (entry in textView.objectLabels.entries) {
                    str = str.replace(entry.key,entry.value)
                }
                str = str.replace("\n","<br>").replace("\r","<br>")
                ignore = true
                textView.text = StringHelper.fromHtml(str)
                ignore = false
            }
        }

        override fun afterTextChanged(s: Editable?) {
            //NOP
        }
    }

    override fun addRule(verb: Int, subject: Int?): SreContextAwareTextView {
        return super.addRule(verb, subject) as SreContextAwareTextView
    }
}