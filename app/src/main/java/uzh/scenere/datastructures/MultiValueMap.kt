package uzh.scenere.datastructures

import java.io.Serializable

class MultiValueMap<K,V>(oldMap: MultiValueMap<K,V>? = null): Serializable {

    val map = HashMap<K,ArrayList<V>>()

    init {
        if (oldMap != null){
            map.putAll(oldMap.map)
        }
    }

    fun put(key: K, value: V){
        if (map.containsKey(key)){
            map[key]!!.add(value)
        }else{
            map[key] = arrayListOf(value)
        }
    }

    fun putAll(key: K, vararg values: V){
        for (v in values){
            put(key,v)
        }
    }

    fun get(key: K): ArrayList<V>{
        return map[key] ?: ArrayList()
    }

    fun getFirst(key: K): V?{
        return map[key]?.get(0)
    }

    fun removeAll(key: K){
        map.remove(key)
    }

    fun remove(key: K, value: V){
        map[key]?.remove(value)
    }

    fun remove(key: K, vararg values: V){
        for (v in values){
            remove(key,v)
        }
    }

    fun getMultiValuedKeys(): List<K>{
        val list = ArrayList<K>()
        for (entry in map){
            if (entry.value.size > 1){
                list.add(entry.key)
            }
        }
        return list
    }

    fun getAllValues(): List<V>{
        val list = ArrayList<V>()
        for (entry in map){
            list.addAll(entry.value)
        }
        return list
    }
}