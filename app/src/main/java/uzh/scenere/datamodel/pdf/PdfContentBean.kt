package uzh.scenere.datamodel.pdf

import uzh.scenere.helpers.StringHelper


class PdfContentBean() {

    private val partEntries = HashMap<String,String>()
    private val tableEntries = HashMap<String,ArrayList<Array<String>>>()

    fun addEntry(key: String, entry: String): PdfContentBean{
        partEntries[key] = entry
        return this
    }

    fun addTable(key: String, entry: ArrayList<Array<String>>): PdfContentBean{
        tableEntries[key] = entry
        return this
    }

    // Replace Website-Title Formatting with PDF-Title Formatting
    private fun addTitleMarkup(text: String?): String? {
        if (text == null) {
            return null
        }
        var formatted: String = text
        formatted = formatted.replace("<h.>".toRegex(), "<b>")
        formatted = formatted.replace("</h.>".toRegex(), "<br>")
        formatted = formatted.replace("<H.>".toRegex(), "<b>")
        formatted = formatted.replace("</H.>".toRegex(), "<br>")
        formatted = formatted.replace("<li>", "- ")
        formatted = formatted.replace("<LI>", "- ")
        formatted = formatted.replace("</li>", "<br>")
        formatted = formatted.replace("</LI>", "<br>")
        return formatted
    }


    private val LINK_BEGIN = "<a href=\""
    private val LINK_END = "</a>"
    private val LINK_MID = "\">"

    private fun parseLinks(textWithLinks: String): String {
        if (textWithLinks.contains(LINK_BEGIN) && textWithLinks.contains(LINK_END)) {
            val firstCut = textWithLinks.split(LINK_BEGIN.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (firstCut.size >= 2) {
                val secondCut = firstCut[1].split(LINK_END.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (secondCut.size >= 2) {
                    val thirdCut = secondCut[0].split(LINK_MID.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (thirdCut.size >= 2) {
                        return firstCut[0] + thirdCut[1] + " (" + thirdCut[0] + ") " + secondCut[1]
                    }
                }
            }
        }
        return textWithLinks
    }

    private val part = "<part_{1}>{2}"
    private val p1 = "{1}"
    private val p2 = "{2}"

    fun getDocumentParts(): Array<String> {
        val partList = ArrayList<String>()
        for (entry in partEntries.entries){
            if (StringHelper.hasText(entry.value)) {
                partList.add(part.replace(p1, entry.key).replace(p2, entry.value))
            }
        }
        return partList.toTypedArray()
    }

    fun getDocumentTables(): HashMap<String,ArrayList<Array<String>>>{
        return tableEntries
    }
}