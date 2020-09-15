package uzh.scenere.datastructures

import uzh.scenere.const.Constants.Companion.FRACTION
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.PERCENT
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.helpers.NumberHelper
import java.util.*

class StatisticArrayList<E> : ArrayList<E>() {
    private val stats = HashMap<E,Float>()

    override fun add(element: E): Boolean {
        stats[element] = (NumberHelper.nvl(stats[element],0f)+1f)
        return super.add(element)
    }

    fun getStatistics(delim: String = NEW_LINE): String{
        val part = 100f/size
        var statistics = NOTHING
        for (entry in stats.entries){
            statistics += "".plus(entry.key).plus(SPACE).plus(NumberHelper.floor(part*entry.value,2)).plus(PERCENT).plus(delim)
        }
        return if (statistics.length > delim.length) statistics.substring(0,statistics.length- delim.length) else statistics
    }

    fun getAsc(): List<E>{
        return getOrdered()
    }

    fun getDesc(): List<E>{
        return getOrdered(true)
    }

    private fun getOrdered(reversed: Boolean = false): ArrayList<E> {
        val list = ArrayList<E>()
        val sortMap = if (reversed) TreeMap<Float, E>(Collections.reverseOrder()) else TreeMap<Float, E>()
        for (entry in stats.entries) {
            var key: Float = entry.value
            while (sortMap.contains(key)) {
                key += FRACTION
            }
            sortMap[key] = entry.key
        }
        for (entry in sortMap.entries){
            list.add(entry.value)
        }
        return list
    }

    fun getPercentage(elem: E): Float{
        val part = 100f/size
        val count = stats[elem]
        return if (count == null) 0f else count*part
    }

    @Suppress("UNCHECKED_CAST")
    fun total(): E?{
        if (!isEmpty() && get(0) is Number){
            var total: Number = 0.0
            when(get(0)){
                is Double -> {
                    total = total.toDouble()
                    for (element in this){
                        total += element as Double
                    }
                }
                is Long -> {
                    total = total.toLong()
                    for (element in this){
                        total += element as Long
                    }
                }
                is Float -> {
                    total = total.toFloat()
                    for (element in this){
                        total += element as Float
                    }
                }
                is Int -> {
                    total = total.toInt()
                    for (element in this){
                        total += element as Int
                    }
                }
                else -> return null
            }
            return total as E
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun avg(): E? {
        val total = total()
        if (total != null){
            when (total){
                is Double ->  return (total as Double/size) as E
                is Long ->  return (total as Long/size) as E
                is Float ->  return (total as Float/size) as E
                is Int ->  return (total as Int/size) as E
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun avgLarge(): E?{
        if (!isEmpty() && get(0) is Number){
            var avg: Double = 0.0
            when(get(0)){
                is Double -> {
                    for (element in this){
                        avg += (element as Double).div(size)
                    }
                    return avg as E
                }
                is Long -> {
                    for (element in this){
                        avg += (element as Long).toDouble().div(size)
                    }
                    return avg.toLong() as E
                }
                is Float -> {
                    for (element in this){
                        avg += (element as Float).toDouble().div(size)
                    }
                    return avg.toFloat() as E
                }
                is Int -> {
                    for (element in this){
                        avg += (element as Int).toDouble().div(size)
                    }
                    return avg.toInt() as E
                }
                else -> return null
            }
        }
        return null
    }
}