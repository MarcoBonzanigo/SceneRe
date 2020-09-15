package uzh.scenere.datamodel.triggers.direct

import uzh.scenere.datamodel.triggers.AbstractTrigger
import uzh.scenere.datamodel.triggers.IDirectTrigger
import uzh.scenere.helpers.NumberHelper
import java.util.*
import kotlin.collections.ArrayList
import java.util.regex.Pattern


class IfElseTrigger(id: String?, previousId: String?, pathId: String, val text: String, val defaultOption: String): AbstractTrigger(id ?: UUID.randomUUID().toString(), previousId, pathId), IDirectTrigger {
    var pathOptions = HashMap<Int,String>()
    var optionLayerLink = HashMap<Int,Int>()


    fun addPathOption(label: String?, layer: Int, option: Int): IfElseTrigger{
        if (label != null && optionLayerLink[option] == null) {
            pathOptions[layer] = label
            optionLayerLink[option] = layer
        }
        return this
    }

    fun removePathOption(option: Int): Int?{
        val layer = optionLayerLink[option]
        optionLayerLink.remove(option)
        pathOptions.remove(layer)
        return layer
    }

    fun withPathOptions(options: HashMap<Int,String>): IfElseTrigger{
        pathOptions = options
        return this
    }

    fun withOptionLayerLink(link: HashMap<Int,Int>): IfElseTrigger{
        optionLayerLink = link
        return this
    }

    fun getLayerForOption(option: Int): Int{
        return NumberHelper.nvl(optionLayerLink[option],-1)
    }

    fun getOptionCount(): Int {
        return optionLayerLink.size
    }

    fun getOptions(): Array<String>{
        val list = ArrayList<String>()
        for (c in 0 until MAX_PATH_COUNT){
            if (optionLayerLink[c] != null && pathOptions[optionLayerLink[c]] != null){
                list.add(pathOptions[optionLayerLink[c]]!!)
            }
        }
        return list.toTypedArray()
    }

    fun getIndexedOptions(): Array<String>{
        val list = ArrayList<String>()
        for (c in 0 until MAX_PATH_COUNT){
            if (optionLayerLink[c] != null && pathOptions[optionLayerLink[c]] != null){
                list.add("[$c] ".plus(pathOptions[optionLayerLink[c]]!!))
            }
        }
        return list.toTypedArray()
    }

    fun getDeletableIndexedOptions(): Array<String>{
        val list = ArrayList<String>()
        for (c in 1 until MAX_PATH_COUNT){
            if (optionLayerLink[c] != null && pathOptions[optionLayerLink[c]] != null){
                list.add("[$c] ".plus(pathOptions[optionLayerLink[c]]!!))
            }
        }
        return list.toTypedArray()
    }

    fun getOptionFromIndexedString(indexedString: String): Int{
        val matcher = Pattern.compile("\\[(.*?)]").matcher(indexedString)
        if (matcher.find()){
            return matcher.group(1).toInt()
        }
        return -1
    }

    companion object {
        const val MAX_PATH_COUNT: Int = 6
    }
}