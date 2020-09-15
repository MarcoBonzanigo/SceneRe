package uzh.scenere.helpers

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.google.android.gms.maps.model.LatLng
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.FILE_TYPE_TXT
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.NULL_CLASS
import uzh.scenere.const.Constants.Companion.REFLECTION
import uzh.scenere.const.Constants.Companion.REPLACEMENT_TOKEN
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.const.Constants.Companion.STYLE
import uzh.scenere.const.Constants.Companion.ZERO
import uzh.scenere.datastructures.SreLatLng
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

inline fun <reified INNER> array2d(sizeOuter: Int, sizeInner: Int, noinline innerInit: (Int)->INNER): Array<Array<INNER>> = Array(sizeOuter) { Array<INNER>(sizeInner, innerInit) }
fun array2dOfInt(sizeOuter: Int, sizeInner: Int): Array<IntArray> = Array(sizeOuter) { IntArray(sizeInner) }
fun array2dOfFloat(sizeOuter: Int, sizeInner: Int): Array<FloatArray> = Array(sizeOuter) { FloatArray(sizeInner) }
fun array2dOfDouble(sizeOuter: Int, sizeInner: Int): Array<DoubleArray> = Array(sizeOuter) { DoubleArray(sizeInner) }
fun array2dOfLong(sizeOuter: Int, sizeInner: Int): Array<LongArray> = Array(sizeOuter) { LongArray(sizeInner) }
fun array2dOfByte(sizeOuter: Int, sizeInner: Int): Array<ByteArray> = Array(sizeOuter) { ByteArray(sizeInner) }
fun array2dOfChar(sizeOuter: Int, sizeInner: Int): Array<CharArray> = Array(sizeOuter) { CharArray(sizeInner) }
fun array2dOfBoolean(sizeOuter: Int, sizeInner: Int): Array<BooleanArray> = Array(sizeOuter) { BooleanArray(sizeInner) }
fun floor(value: Double, precision: Int):Double = Math.floor(precision*value)/precision.toDouble()
fun EditText.getStringValue(): String = text.toString()
fun TextView.getStringValue(): String = text.toString()
fun Random.nextSafeInt(range: Int): Int = if (range<=0) 0 else nextInt(range)
fun Any.className(): String {
    val splits = this::class.toString().replace(REFLECTION,NOTHING).split(".")
    val s = splits[splits.size - 1]
    if (s.startsWith(NULL_CLASS) && !this::class.supertypes.isEmpty()){
        return this::class.supertypes[0].className()
    }
    return s
}
fun Any.readableClassName(delimiter: String = SPACE): String {
    val className = className()
    var readableClassName = ""
    for (c in 0 until className.length){
        if (c>0 && className[c].isUpperCase()){
            readableClassName += delimiter
        }
        readableClassName += className[c]
    }
    return readableClassName
}
fun Cursor.getBoolean(columnIndex: Int): Boolean{
    return getInt(columnIndex) == 1
}
fun countNonNull(vararg args: Any?):Int {
    var count = 0
    for (arg in args){
        if (arg != null){
            count++
        }
    }
    return count
}

@Suppress("UNCHECKED_CAST")
fun addToArrayBefore(array: Array<String>, vararg args: String): Array<String>{
    val newArray = arrayOfNulls<String?>(array.size+args.size)
    var i = 0
    for (t in args){
        newArray[i] = t
        i++
    }
    for (t in array){
        newArray[i] = t
        i++
    }
    return newArray as Array<String>
}

@Suppress("UNCHECKED_CAST")
fun <T: Any> addToArrayAfter(array: Array<T>, vararg args: T): Array<T>{
    val newArray: Array<T> = arrayOfNulls<Any>(array.size+args.size) as Array<T>
    var i = 0
    for (t in array){
        newArray[i] = t
        i++
    }
    for (t in args){
        newArray[i] = t
        i++
    }
    return newArray
}

fun ArrayList<*>.toStringArray(): Array<String>{
    val list = ArrayList<String>()
    for (i in 0 until size){
        list.add(get(i).toString())
    }
    return list.toTypedArray()
}

fun List<*>.toStringArray(): Array<String>{
    val list = ArrayList<String>()
    for (i in 0 until size){
        list.add(get(i).toString())
    }
    return list.toTypedArray()
}

fun String.isContainedIn(str: String?):Boolean{
    if (str == null) return false
    return str.contains(this)
}
fun File.isFileType(type: String): Boolean{
    if (isDirectory) return false
    return name.endsWith(type)
}


fun Activity.getIdByString(str: String, type: String = "string") = resources.getIdentifier(str, type, packageName)

fun Activity.getStringByString(str: String, vararg formatArgs: String):String {
    var txt = getString(getIdByString(str))
    for (id in 0 until formatArgs.size){
        txt = txt.replace("%${id+1}\$s",formatArgs[id])
    }
    return txt
}

fun Activity.getGenericStringWithIdAndTemplate(id: Int, templateId: Int, vararg formatArgs: String):String {
    return getStringByString(getString(templateId,id),*formatArgs)
}

fun ArrayList<*>.removeFirst(){
    if (!isNullOrEmpty()){
        removeAt(0)
    }
}

enum class SreStyle{
    NORMAL, CONTRAST, OLED
}
fun getColorWithStyle(context: Context, id: Int): Int{
    val sreStyle = getSreStyle(context)
    return when {
        sreStyle == SreStyle.NORMAL -> {
            ContextCompat.getColor(context,id)
        }
        sreStyle == SreStyle.CONTRAST -> {
            val color = StyleHelper.get(context).switchToContrastMode[id]
            color ?: ContextCompat.getColor(context,id)
        }
         sreStyle == SreStyle.OLED-> {
             val color = StyleHelper.get(context).switchToOledMode[id]
             color ?: ContextCompat.getColor(context,id)
        }
        else -> ContextCompat.getColor(context,id)
    }
}

fun getSreStyle(context: Context): SreStyle{
    val enum = DatabaseHelper.getInstance(context).read(STYLE, String::class, SreStyle.NORMAL.toString(), DatabaseHelper.DataMode.PREFERENCES)
    return SreStyle.valueOf(enum)
}

fun resolveSpinnerLayoutStyle(context: Context): Int {
    val sreStyle = getSreStyle(context)
    when (sreStyle){
        SreStyle.NORMAL -> return R.layout.sre_spinner_item
        SreStyle.CONTRAST -> return R.layout.sre_spinner_item_contrast
        SreStyle.OLED -> return R.layout.sre_spinner_item_oled
    }
}

fun reStyle(context: Context, root: ViewGroup?, sreStyle: SreStyle? = null) {
    var style = sreStyle
    if (sreStyle == null){
        style = getSreStyle(context)
    }
    if (root != null && root.childCount != 0) {
        for (view in 0 until root.childCount){
            val childAt = root.getChildAt(view)
            if (childAt is ViewGroup){
                reStyle(context, childAt, style)
            }
            reStyleColors(context, childAt, style!!)
        }
        reStyleColors(context, root, style!!)
    }else if (root != null){
        reStyleColors(context, root, style!!)
    }
}

private fun reStyleColors(context: Context, view: View, sreStyle: SreStyle){
    StyleHelper.get(context).switchColors(context,view,sreStyle)
}

enum class WhatIfMode(){
    ALL, DYNAMIC, STAKEHOLDER, OBJECTS, STATIC, NONE
}

fun <T: Any?> T?.notNull(f: ()-> Unit): T?{
    if (this != null){
        f()
    }
    return this
}

fun <T: Any> HashMap<T,Int>.addOne(key: T){
    this[key] = NumberHelper.nvl(this[key], ZERO).plus(Constants.ONE)
}

fun <K,V: Comparable<V>> Map<K,V>.sortByValue(): TreeMap<K, V> {
    val comparator = Comparator<K> { k1, k2 ->
        val compare = get(k1)!!.compareTo(get(k2)!!)
        if (compare == 0) 1 else compare
    }
    val sorted = TreeMap<K,V>(comparator)
    sorted.putAll(this)
    return sorted
}

fun <K,V: Comparable<V>> Map<K,V>.sort(): TreeMap<K, V> {
    val sorted = TreeMap<K,V>()
    sorted.putAll(this)
    return sorted
}

fun LatLng.toSreLatLng() = SreLatLng(this)

fun getDrawableIdByName(context: Context, drawableName: String): Int {
    return context.resources.getIdentifier(drawableName, "drawable", context.packageName)
}

fun readTextFileFromAsset(context: Context, fileName: String): String{
    var reader: BufferedReader? = null
    try {
        reader = BufferedReader(InputStreamReader(context.assets.open(fileName+ FILE_TYPE_TXT)));

        val builder = StringBuilder()
        var line: String? = null
        do {
            line = reader.readLine()
            if (line != null){
                builder.append(line)
            }
        }while (line != null)
        return builder.toString()
    } catch (e: Exception) {
    } finally {
        if (reader != null) {
            try {
                reader.close()
            } catch (e: Exception) {
            }
        }
    }
    return NOTHING
}

fun String.replaceAllIgnoreCase(str: String, replaceWith: String): String{
    if (str == replaceWith){
        return this
    }
    var returnString = this
    val strLowerCase = str.toLowerCase()
    var thisLowerCase = this.toLowerCase()
    var replacement = replaceWith
    var identity = false
    if (replaceWith.toLowerCase().contains(strLowerCase)){
        replacement = REPLACEMENT_TOKEN
        identity = true
    }
    var index = thisLowerCase.indexOf(strLowerCase)
    while (index >= 0){
        returnString = returnString.replaceRange(index,index+str.length,replacement)
        thisLowerCase = thisLowerCase.replaceRange(index,index+str.length,replacement)
        index = thisLowerCase.indexOf(strLowerCase)
    }
    return if (identity) returnString.replace(REPLACEMENT_TOKEN, replaceWith) else returnString
}

fun Any.getAllDeclaredFields(): ArrayList<Field>{
    val fields = ArrayList<Field>()
    var currentClass: Class<in Any> = javaClass
    while(currentClass.superclass !=null){
        fields.addAll(currentClass.declaredFields)
        currentClass = currentClass.superclass
    }
    return fields
}