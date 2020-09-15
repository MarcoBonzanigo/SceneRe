package uzh.scenere.helpers

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.*
import android.text.style.MetricAffectingSpan
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.CARRIAGE_RETURN
import uzh.scenere.const.Constants.Companion.COMMA
import uzh.scenere.const.Constants.Companion.COMMA_DELIM
import uzh.scenere.const.Constants.Companion.COMMA_TOKEN
import uzh.scenere.const.Constants.Companion.DAY_MS
import uzh.scenere.const.Constants.Companion.HOUR_MS
import uzh.scenere.const.Constants.Companion.MIN_MS
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.SEC_MS
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.const.Constants.Companion.ZERO_L
import uzh.scenere.datamodel.AbstractObject
import uzh.scenere.datamodel.Stakeholder
import uzh.scenere.datamodel.steps.AbstractStep
import java.io.Serializable


class StringHelper{
    companion object { //Static Reference
        fun <T : Serializable> concatTokens(delimiter: String, obj: List<T>): String {
            if (!obj.isEmpty()) {
                var conc = ""
                for (o in obj) {
                    conc += if (o is String) {
                        o + delimiter
                    } else {
                        o.toString() + delimiter
                    }
                }
                return conc.substring(0, conc.length - delimiter.length)
            }
            return NOTHING
        }

        fun concatTokens(delimiter: String, vararg obj: Serializable): String {
            if (!obj.isEmpty()) {
                var conc = ""
                for (o in obj) {
                    conc += if (o is String) {
                        o + delimiter
                    } else {
                        o.toString() + delimiter
                    }
                }
                return conc.substring(0, conc.length - delimiter.length)
            }
            return NOTHING
        }

        fun concatTokensForCsv(vararg tokens: String): String {
            if (tokens.isEmpty()){
                return NEW_LINE
            }
            var str = NOTHING
            for (token in tokens){
                str += token.replace(COMMA, NOTHING).replace(NEW_LINE, SPACE).replace(CARRIAGE_RETURN, SPACE).replace(COMMA_TOKEN, COMMA)+COMMA
            }
            return str.substring(0, str.length - COMMA.length)+ NEW_LINE
        }

        fun concatList(delimiter: String, obj: List<String>): String{
            if (!obj.isEmpty()) {
                var conc = ""
                for (o in obj) {
                    conc += o + delimiter
                }
                return conc.substring(0, conc.length - delimiter.length)
            }
            return NOTHING
        }

        fun concatListWithoutIdBrackets(delimiter: String, obj: List<String>): String {
            if (!obj.isEmpty()) {
                var conc = ""
                for (o in obj) {
                    val split = o.split("[")
                    conc += split[0] + delimiter
                }
                return conc.substring(0, conc.length - delimiter.length)
            }
            return NOTHING
        }

        fun concatWithIdBrackets(str: String, id: Int): String{
            return "$str[$id]"
        }

        fun concatWithIdBrackets(str: String, id: String): String{
            return "$str[$id]"
        }

        fun nvl(value: String?, valueIfNull: String): String {
            return if (hasText(value)) value!! else  valueIfNull
        }

        fun lookupOrEmpty(id: Int?, applicationContext: Context?): CharSequence? {
            return if (id==null) "" else applicationContext?.resources?.getString(id)
        }

        fun hasText(text: Editable?): Boolean {
            if (text == null) return false
            return hasText(text.toString())
        }

        fun hasText(text: String?): Boolean {
            return (text != null && text.isNotBlank())
        }

        fun hasText(text: CharSequence?): Boolean {
            return (text != null && text.isNotBlank())
        }

        fun stripBlank(text: String): String{
            return text.replace(SPACE,NOTHING).replace(CARRIAGE_RETURN,NOTHING).replace(NEW_LINE,NOTHING)
        }

        fun extractNameFromClassString(className: String): String{
            val split = className.split(".")
            return split[split.size-1]
        }

        fun styleString(stringId: Int, context: Context, appendBefore: ArrayList<String> = ArrayList(), appendAfter: ArrayList<String> = ArrayList()): SpannableString {
            val builder = SpannableStringBuilder()
            for (string in appendBefore){
                builder.append(string)
            }
            builder.append(StringHelper.styleString(context.getText(stringId) as SpannedString, Typeface.createFromAsset(context.assets, "FontAwesome900.otf")))
            for (string in appendAfter){
                builder.append(string)
            }
            return SpannableString.valueOf(builder)
        }

        fun styleString(spannedString: SpannedString, typeface: Typeface?): SpannableString {
            val annotations = spannedString.getSpans(0, spannedString.length, android.text.Annotation::class.java)
            val spannableString = SpannableString(spannedString)
            for (annotation in annotations) {
                if (annotation.key == "font") {
                    val fontName = annotation.value
                    if (fontName == "font_awesome") {
                        spannableString.setSpan(CustomTypefaceSpan(typeface),
                                spannedString.getSpanStart(annotation),
                                spannedString.getSpanEnd(annotation),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }
            return spannableString
        }

        class CustomTypefaceSpan(private val typeface: Typeface?) : MetricAffectingSpan() {

            override fun updateDrawState(drawState: TextPaint) {
                apply(drawState)
            }

            override fun updateMeasureState(paint: TextPaint) {
                apply(paint)
            }

            private fun apply(paint: Paint) {
                val oldTypeface = paint.typeface
                val oldStyle = if (oldTypeface != null) oldTypeface.style else 0
                val fakeStyle = oldStyle and typeface!!.style.inv()

                if (fakeStyle and Typeface.BOLD != 0) {
                    paint.isFakeBoldText = true
                }

                if (fakeStyle and Typeface.ITALIC != 0) {
                    paint.textSkewX = -0.25f
                }

                paint.typeface = typeface
            }

        }

        fun fromHtml(html: String?): Spanned {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(html)
            }
        }

        fun countSubstrings(string: String, substring: String): Int{
            var count = 0
            var original = string
            var replaced = string
            var finished = false
            do {
                replaced = original.replaceFirst(substring, NOTHING)
                if (replaced != original){
                    original = replaced
                    count++
                }else{
                    finished = true
                }
            }while(!finished)
            return count
        }

        fun substringAfterOccurrences(string: String, substring: String, occurrences: Int): String{
            if (string.length > substring.length) {
                var count = 0
                for (index in 0 until (string.length - substring.length)) {
                    if (string.substring(index,index+substring.length) == substring){
                        count++
                    }
                    if (count >= occurrences){
                        return string.substring(0,index)
                    }
                }
            }
            return string
        }

        fun cutHtmlAfter(html: String, linesMax: Int, hint:String): String{
            val lines = countSubstrings(html, "<br>")
            if (lines > linesMax){
                return substringAfterOccurrences(html,"<br>",linesMax)+"<br>"+hint
            }
            return html
        }

        fun numberToPositionString(num: Int): String {
            val i = num%10
            return when (i){
                1 -> num.toString().plus("st")
                2 -> num.toString().plus("nd")
                3 -> num.toString().plus("rd")
                else -> num.toString().plus("th")
            }
        }

        fun applyFilters(str: String?, context: Context): String{
            if (!hasText(str)){
                return NOTHING
            }
            return str!!.replace(context.getString(R.string.filter_optional), Constants.NOTHING)
                    .replace(context.getString(R.string.filter_regex), Constants.NOTHING)
                    .replace(context.getString(R.string.filter_s), Constants.NOTHING)
                    .replace(context.getString(R.string.filter_m), Constants.NOTHING)
                    .replace(context.getString(R.string.filter_dB), Constants.NOTHING)
                    .replace(context.getString(R.string.filter_next_step), Constants.NOTHING)
                    .replace(context.getString(R.string.editor_gps_warning_replace), context.getString(R.string.editor_gps_formatting))
        }

        fun msToFormattedString(ms: Long): String{
            var remain = ms
            var days = ZERO_L
            var hours = ZERO_L
            var minutes = ZERO_L
            var seconds = ZERO_L
            if (ms > DAY_MS){
                days = remain.div(DAY_MS)
                remain -= days.times(DAY_MS)
            }
            if (ms > HOUR_MS){
                hours = remain.div(HOUR_MS)
                remain -= hours.times(HOUR_MS)
            }
            if (ms > MIN_MS){
                minutes = remain.div(MIN_MS)
                remain -= minutes.times(MIN_MS)
            }
            if (ms > SEC_MS){
                seconds = remain.div(SEC_MS)
                remain -= seconds.times(SEC_MS)
            }
            return (when {
                days != ZERO_L -> "$days d, $hours h, $minutes min, $seconds sec"
                hours != ZERO_L -> "$hours h, $minutes min, $seconds sec"
                minutes != ZERO_L -> "$minutes min, $seconds sec"
                else -> "$seconds sec"
            })
        }

        fun toListString(entries: ArrayList<out Serializable>, delim: String = COMMA_DELIM): String {
            val list = ArrayList<String>()
            for (entry in entries){
                when (entry){
                    is AbstractStep -> if (entry.title != null) list.add(entry.title!!)
                    is AbstractObject -> list.add(entry.name)
                    is Stakeholder -> list.add(entry.name)
                }
            }
            return concatList(delim,list)
        }

        fun isAQuestion(context: Context, text: String): Boolean{
            var isQuestion = false
            val lowerCase = text.toLowerCase()
            for (word in context.resources.getStringArray(R.array.question_words)){
                if (lowerCase.contains(word)){
                    isQuestion = true
                }
            }
            val qMark = lowerCase.contains("?")
            return qMark || isQuestion
        }
    }
}