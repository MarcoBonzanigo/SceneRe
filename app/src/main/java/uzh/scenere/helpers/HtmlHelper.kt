package uzh.scenere.helpers

import java.util.regex.Pattern

class HtmlHelper private constructor() {
    companion object {


        private val from = arrayOf("ä", "Ä", "ö", "Ö", "ü", "Ü", "ß", "€", "&", "<", ">")
        private val to = arrayOf("&auml;", "&Auml;", "&ouml;", "&Ouml;", "&uuml;", "&Uuml;", "&szlig;", "&euro;", "&amp;", "&lt;", "&gt;", "&quot;")

        fun modifyUmlauts(source: String): String {
            var src = source
            for (p in from.indices) {
                src = src.replace(from[p], to[p])
            }
            return src
        }

        fun modifyUmlautsReverse(source: String): String {
            var src = source
            for (p in from.indices) {
                src = src.replace(to[p], from[p])
            }
            return src
        }

        private val headerPattern = Pattern.compile("[0-9].[0-9].[0-9]")
        private val titlePattern = Pattern.compile("^[a-zA-ZäöüÄÖÜ][a-zA-Z- äöüÄÖÜ]*:")
        private val listPattern = Pattern.compile("^-[a-zA-Z]*")

        fun formatForReleaseNotes(source: String): String {
            val split = source.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val sb = StringBuilder()
            for (str in split) {
                var s = str
                if (headerPattern.matcher(s).find()) {
                    s = concatenateTokens("<b>", "<u>", s, "</u>", "</b>", "<br>")
                } else if (titlePattern.matcher(s).find()) {
                    s = concatenateTokens("<b>", s, "</b>", "<br>")
                } else if (listPattern.matcher(s).find()) {
                    s = concatenateTokens("<i>", s.replaceFirst("-".toRegex(), ""), "</i>", "<br>")
                } else {
                    s = stripLineBreaks(s) + "<br>"
                }
                sb.append(s)
                sb.append(System.lineSeparator())
            }
            return sb.toString()
        }

        private fun concatenateTokens(vararg strings: String): String {
            val sb = StringBuilder()
            for (s in strings) {
                sb.append(stripLineBreaks(s))
            }
            return sb.toString()
        }


        private fun stripLineBreaks(s: String): String {
            return s.replace("\n", "").replace("\r", "").replace(System.lineSeparator(), "")
        }

        fun stripHtmlTags(s: String): String {
            return s.replace("<[^>]*>".toRegex(), "")
        }

        fun stripLinks(s: String): String {
            return s.replace("(?i)<a[^>]*>".toRegex(), "")
        }

    }
}