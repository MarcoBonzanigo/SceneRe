@file:Suppress("UNCHECKED_CAST","unused")
package uzh.scenere.helpers

import android.content.Context
import android.graphics.BitmapFactory
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import uzh.scenere.const.Constants.Companion.BOLD_TOKEN
import uzh.scenere.const.Constants.Companion.BRACKET_CLOSE
import uzh.scenere.const.Constants.Companion.BREAK_TOKEN
import uzh.scenere.const.Constants.Companion.FILE_TYPE_TFF
import uzh.scenere.const.Constants.Companion.FONT_HELVETICA_BOLD
import uzh.scenere.const.Constants.Companion.FONT_HELVETICA_NORMAL
import uzh.scenere.const.Constants.Companion.IMAGE_TOKEN
import uzh.scenere.const.Constants.Companion.LIST_BEGIN
import uzh.scenere.const.Constants.Companion.MARGIN_TOKEN
import uzh.scenere.const.Constants.Companion.NEW_LINE
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.PART_TOKEN
import uzh.scenere.const.Constants.Companion.QUOTE
import uzh.scenere.const.Constants.Companion.SPACE
import uzh.scenere.const.Constants.Companion.TABLE_TOKEN
import uzh.scenere.datamodel.pdf.PdfConstructor
import uzh.scenere.datamodel.pdf.PdfContentBean
import uzh.scenere.datamodel.pdf.PdfFileBean
import uzh.scenere.datamodel.pdf.PdfLineConfiguration
import java.io.IOException
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList


class PdfHelper(val context: Context) {

    /**
     * SYNTAX: Linebreaks are made with <br></br>
     * Other than that, the following markups are possible:
     */
    // Patterns
    private val imagePattern = Pattern.compile("<image_.*>") // e.g. <image_uzh_eth_logo>, placeholder for the image "uzh_eth_logo"
    private val partPattern = Pattern.compile("<part_.*>") // e.g. <part_dates>, placeholder for "dates"
    private val partPostTitlePattern = Pattern.compile("<part_[a-zA-Z]+\"") // e.q. <part_comment", Kommentar.">, ph for "comment" with post title
    private val partPreTitlePattern = Pattern.compile("<part_\".*") // e.g. <part_"Kommentar: "comment>, ph for "comment" with pre title
    private val marginPattern = Pattern.compile("<margin_[0-9]*,[0-9]*,[0-9]*,[0-9]*>") // e.g. <margin_50,50,50,50>, margin of 50 t,b,l,r
    private val defaultSizePattern = Pattern.compile("<ds[0-9]*>")// e.g. <ds9>
    private val tablePattern = Pattern.compile("<table_.*>") // e.g. <table_praesenz>, Table called "praesenz"
    private val datePattern = Pattern.compile("<date>")
    private val rightAlignPattern = Pattern.compile("<ra>")
    private val centerAlignPattern = Pattern.compile("<ca>")
    private val bottomLeftAlignPattern = Pattern.compile("<bla>")
    private val bottomRightAlignPattern = Pattern.compile("<bra>")
    private val sizePattern = Pattern.compile("<s[0-9]*>") // e.g. <s10>
    private val boldPattern = Pattern.compile("<b>")
    private val boldSegmentPattern = Pattern.compile("<b_[0-9]*,[0-9]*>") // e.g. <b_0,1> first word bold
    private val pageBreakPattern = Pattern.compile("<pb>")
    private val breakBelowPattern = Pattern.compile("<breakbelow_[0-9]*>") // adds a new page if added content is too low
    // Files

    fun fileToPdf(fileName: String, content: String) {
        val pdfConstructor = createPdfConstructor(content, null)
        val renderDocumentToBytesInternal = renderDocumentToBytesInternal(pdfConstructor)
        val writeFile = FileHelper.writeFile(context, renderDocumentToBytesInternal, fileName)
        FileHelper.openFile(context,writeFile)
    }

    fun renderPdf(contentBean: PdfContentBean): PdfFileBean {
        val fileBean = PdfFileBean()
        var pdfConstructor: PdfConstructor? = null
        var pdfByteContent = ByteArray(0)
        val content = readTextFileFromAsset(context,"pdf_body")
        pdfConstructor = createPdfConstructor(content, pdfConstructor)
        if (pdfConstructor != null) {
            pdfConstructor.setParts(contentBean.getDocumentParts())
            for (table in contentBean.getDocumentTables()){
                pdfConstructor.addTableData(table.key,table.value)
            }
        }
        pdfByteContent = renderDocumentToBytesInternal(pdfConstructor)
        fileBean.addFile(pdfByteContent, "pdf_export", true)
        fileBean.finish()
        return fileBean
    }

    private fun createNumberedColumns(numbers: Int, offset: Int): Array<String> {
        val numberedColumns = arrayOfNulls<String>(numbers + offset)
        for (i in offset until numberedColumns.size) {
            numberedColumns[i] = SPACE + (if (i + 1 - offset < 10) SPACE else NOTHING) + (i + 1 - offset) + SPACE
        }
        return numberedColumns as Array<String>
    }

    /**
     * Always returns the most recent document state.<br></br>
     * Use the last output if you render the whole content into one file.
     *
     * @param pdfConstructor
     * @return
     */
    private fun renderDocumentToBytesInternal(pdfConstructor: PdfConstructor?): ByteArray {
        if (pdfConstructor == null) {
            return ByteArray(0)
        }
        try {
            parseLines(pdfConstructor)
            pdfConstructor.closeContentStream()
            return pdfConstructor.getBytes()
        } catch (e: IOException) {
            //NOP
        }

        return ByteArray(0)
    }

    private fun renderDocumentToPDDInternal(pdfConstructor: PdfConstructor?): PDDocument? {
        if (pdfConstructor == null) {
            return null
        }
        try {
            parseLines(pdfConstructor)
            pdfConstructor.closeContentStream()
            return pdfConstructor.getDocument()
        } catch (e: IOException) {
        }

        return null
    }
    
    /**
     * The oldContent is used if you want to render the Content to one File only<br></br>
     * Otherwise pass null.
     *
     * @param content
     * @param oldContent
     * @return
     */
    private fun createPdfConstructor(content: String, oldContent: PdfConstructor?): PdfConstructor? {
        try {
            var document: PDDocument? = null
            val page = PDPage()
            if (oldContent == null) {
                document = PDDocument()
            } else {
                document = oldContent.getDocument()
            }
            document.addPage(page)
            val contentStream: PDPageContentStream
            contentStream = PDPageContentStream(document, page)
            val fontSize = getDefaultSize(content)
            val font: PDType0Font = PDType0Font.load(document,context.assets.open(FONT_HELVETICA_NORMAL + FILE_TYPE_TFF))
            val fontBold: PDType0Font = PDType0Font.load(document,context.assets.open(FONT_HELVETICA_BOLD + FILE_TYPE_TFF))
            val alignment = PdfLineConfiguration.Alignment.LEFT
            val config = PdfLineConfiguration.Builder(NOTHING, alignment)
                    .withMargin(50f, 50f, 70f, 70f)
                    .onLine(0f)
                    .build()
            val pdfConstructor = PdfConstructor(config, document, page, contentStream, font, fontBold, fontSize)
            val lines = prepareText(content)

            val linePointer = 0f
            pdfConstructor.getConfig().setLine(linePointer)
            pdfConstructor.setLines(lines)
            return pdfConstructor
        } catch (e: IOException) {
        }

        return null
    }

    private fun getDefaultSize(content: String): Float {
        var defaultFontSize = 9f
        if (defaultSizePattern.matcher(content).find()) {
            val substring = content.substring(content.lastIndexOf("<ds"), content.length)
            val size = substring.substring(substring.indexOf("<ds") + 3, substring.indexOf(BRACKET_CLOSE))
            defaultFontSize = size.toFloat()
        }
        return defaultFontSize
    }

    @Throws(IOException::class)
    private fun parseLines(pdfConstructor: PdfConstructor) {
        // Parse Top-Down
        for (s in pdfConstructor.getLines()) {
            var str = s
            var actualFontSize = pdfConstructor.getFontSize()
            if (breakBelowPattern.matcher(str).find()) {
                handleBreakBelow(pdfConstructor, str)
            }
            if (marginPattern.matcher(str).find()) {
                handleMargin(pdfConstructor, str)
            }
            if (imagePattern.matcher(str).find()) {
                handleImage(pdfConstructor, str)
                continue
            }
            if (tablePattern.matcher(str).find()) {
                handleTable(pdfConstructor, str)
                continue
            }
            if (pageBreakPattern.matcher(str).find()) {
                handlePageBreak(pdfConstructor)
                continue
            }
            if (sizePattern.matcher(str).find()) {
                actualFontSize = handleSize(str)
            }
            if (boldPattern.matcher(str).find()) {
                pdfConstructor.setUseBoldFont(true)
            } else {
                pdfConstructor.setUseBoldFont(false)
            }
            if (boldSegmentPattern.matcher(str).find()) {
                handleBoldSegment(pdfConstructor, str)
            }
            if (partPattern.matcher(str).find()) {
                str = handleParts(pdfConstructor, str)
            }
            if (partPostTitlePattern.matcher(str).find()) {
                str = handlePostTitleParts(pdfConstructor, str)
            }
            if (partPreTitlePattern.matcher(str).find()) {
                str = handlePreTitleParts(pdfConstructor, str)
            }
            if (datePattern.matcher(str).find()) {
                str = handleDate(str)
            }
            if (rightAlignPattern.matcher(str).find()) {
                pdfConstructor.getConfig().setAlignment(PdfLineConfiguration.Alignment.RIGHT)
            } else if (centerAlignPattern.matcher(str).find()) {
                pdfConstructor.getConfig().setAlignment(PdfLineConfiguration.Alignment.CENTER)
            } else if (bottomLeftAlignPattern.matcher(str).find() || bottomRightAlignPattern.matcher(str).find()) {
                continue
            } else {
                pdfConstructor.getConfig().setAlignment(PdfLineConfiguration.Alignment.LEFT)
            }
            pdfConstructor.setConfig(PdfLineConfiguration.Builder(HtmlHelper.stripHtmlTags(str), pdfConstructor.getConfig().getAlignment())
                    .withMargin(pdfConstructor.getConfig().getMarginTop(), pdfConstructor.getConfig().getMarginBottom(), pdfConstructor.getConfig().getMarginLeft(), pdfConstructor.getConfig().getMarginRight())
                    .onLine(pdfConstructor.getConfig().getLine())
                    .withBoldSection(pdfConstructor.getConfig().getBoldSegmentStart(), pdfConstructor.getConfig().getBoldSegmentEnd())
                    .build())
            pdfConstructor.setFontSize(actualFontSize)
            pdfConstructor.getConfig().setLine(drawString(pdfConstructor))
            pdfConstructor.revertToDefaultFontSize()
            if (pdfConstructor.getConfig().getLine() < pdfConstructor.getConfig().getMarginBottom()) {
                if (pdfConstructor.getDocument().getPages().count == 1) {
                    drawPageNumber(pdfConstructor)
                }
                pdfConstructor.getContentStream().close()
                val nextPage = PDPage()
                pdfConstructor.getDocument().addPage(nextPage)
                pdfConstructor.setContentStream(PDPageContentStream(pdfConstructor.getDocument(), nextPage))
                pdfConstructor.getConfig().setLine(0f)
                drawPageNumber(pdfConstructor)
            }
        }
        // Parse Bottom-Up
        val lineCache = pdfConstructor.getConfig().getLine()
        pdfConstructor.getConfig().setLine(0f)
        for (i in pdfConstructor.getLines().size - 1 downTo 0) {
            var str = pdfConstructor.getLines()[i]
            var actualFontSize = pdfConstructor.getFontSize()
            val actualFont = pdfConstructor.getFont()
            if (marginPattern.matcher(str).find()) {
                handleMargin(pdfConstructor, str)
            }
            if (sizePattern.matcher(str).find()) {
                actualFontSize = handleSize(str)
            }
            if (boldPattern.matcher(str).find()) {
                pdfConstructor.setUseBoldFont(true)
            } else {
                pdfConstructor.setUseBoldFont(false)
            }
            if (boldSegmentPattern.matcher(str).find()) {
                handleBoldSegment(pdfConstructor, str)
            }
            if (bottomLeftAlignPattern.matcher(str).find()) {
                pdfConstructor.getConfig().setAlignment(PdfLineConfiguration.Alignment.BOTTOM_LEFT)
                if (partPattern.matcher(str).find()) {
                    str = handleParts(pdfConstructor, str)
                }
                if (partPostTitlePattern.matcher(str).find()) {
                    str = handlePostTitleParts(pdfConstructor, str)
                }
                if (partPreTitlePattern.matcher(str).find()) {
                    str = handlePreTitleParts(pdfConstructor, str)
                }
                if (datePattern.matcher(str).find()) {
                    str = handleDate(str)
                }
            } else if (bottomRightAlignPattern.matcher(str).find()) {
                pdfConstructor.getConfig().setAlignment(PdfLineConfiguration.Alignment.BOTTOM_RIGHT)
                if (partPattern.matcher(str).find()) {
                    str = handleParts(pdfConstructor, str)
                }
                if (partPostTitlePattern.matcher(str).find()) {
                    str = handlePostTitleParts(pdfConstructor, str)
                }
                if (partPreTitlePattern.matcher(str).find()) {
                    str = handlePreTitleParts(pdfConstructor, str)
                }
                if (datePattern.matcher(str).find()) {
                    str = handleDate(str)
                }
            } else {
                continue
            }
            pdfConstructor.setConfig(PdfLineConfiguration.Builder(HtmlHelper.stripHtmlTags(str), pdfConstructor.getConfig().getAlignment())
                    .withMargin(pdfConstructor.getConfig().getMarginTop(), pdfConstructor.getConfig().getMarginBottom(), pdfConstructor.getConfig().getMarginLeft(), pdfConstructor.getConfig().getMarginRight())
                    .onLine(pdfConstructor.getConfig().getLine())
                    .withBoldSection(pdfConstructor.getConfig().getBoldSegmentStart(), pdfConstructor.getConfig().getBoldSegmentEnd())
                    .withDirection(PdfLineConfiguration.Direction.BACKWARD)
                    .build())
            pdfConstructor.setFont(actualFont)
            pdfConstructor.setFontSize(actualFontSize)
            pdfConstructor.getConfig().setLine(drawString(pdfConstructor))
        }
        pdfConstructor.getConfig().setLine(lineCache)
    }

    @Throws(IOException::class)
    private fun drawPageNumber(pdfConstructor: PdfConstructor) {
        pdfConstructor.getContentStream().beginText()
        pdfConstructor.getContentStream().newLineAtOffset(pdfConstructor.getPage().mediaBox.width - pdfConstructor.getConfig().getMarginRight() / 2f, pdfConstructor.getConfig().getMarginBottom() / 2f)
        pdfConstructor.getContentStream().setFont(pdfConstructor.getFont(), pdfConstructor.getDefaultFontSize())
        pdfConstructor.getContentStream().showText(pdfConstructor.getDocument().pages.count.toString())
        pdfConstructor.getContentStream().endText()
    }

    private fun prepareText(text: String): Array<String> {
        var t = text
        if (!StringHelper.hasText(t)) {
            return arrayOf(NOTHING)
        }
        t = t.replace("\uFFFD".toRegex(), "\"")
        t = t.replace("\u0009".toRegex(), SPACE)
        t = t.replace("\\R".toRegex(), NOTHING)
        t = t.replace("\r".toRegex(), NOTHING)
        t = t.replace(NEW_LINE.toRegex(), NOTHING)
        return if (t.contains("<br>")) {
            t.split("<br>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else arrayOf(t)
    }

    @Throws(IOException::class)
    fun checkStringLength(pdfConstructor: PdfConstructor, vararg lines: String): Array<String> {
        val font = if (pdfConstructor.isUseBoldFont()) pdfConstructor.getFontBold() else pdfConstructor.getFont()
        val newLines = ArrayList<String>()
        for (l in lines) {
            val line = HtmlHelper.stripLinks(l)
            for (preparedLine in prepareText(line)) {
                val lineWidth = font.getStringWidth(preparedLine) / 1000 * pdfConstructor.getFontSize()
                val pageWidth = pdfConstructor.getPage().mediaBox.width - pdfConstructor.getConfig().getMarginLeft() - pdfConstructor.getConfig().getMarginRight()
                if (lineWidth > pageWidth) {
                    cutSentences(pdfConstructor, font, newLines, preparedLine, pageWidth)
                } else {
                    newLines.add(preparedLine)
                }
            }
        }
        return newLines.toArray(arrayOfNulls<String>(newLines.size))
    }

    fun checkStringLengthForTable(pdfConstructor: PdfConstructor, line: String, alreadyExistingColumnWidths: List<Float>): Array<String> {
        val font = if (pdfConstructor.isUseBoldFont()) pdfConstructor.getFontBold() else pdfConstructor.getFont()
        val newLines = ArrayList<String>()
        var width = 0f
        for (i in alreadyExistingColumnWidths){
            width += i
        }
        width *= 1.6f
        for (preparedLine in prepareText(line)) {
            val lineWidth = font.getStringWidth(preparedLine) / 1000 * pdfConstructor.getFontSize()
            val pageWidth = pdfConstructor.getPage().mediaBox.width - pdfConstructor.getConfig().getMarginLeft() - pdfConstructor.getConfig().getMarginRight() - width
            if (lineWidth > pageWidth) {
                cutSentences(pdfConstructor, font, newLines, preparedLine, pageWidth)
            } else {
                newLines.add(preparedLine)
            }
        }
        return newLines.toArray(arrayOfNulls<String>(newLines.size))
    }


    @Throws(IOException::class)
    private fun cutSentences(pdfConstructor: PdfConstructor, font: PDType0Font, newLines: ArrayList<String>, line: String, pageWidth: Float) {
        val words = line.split(SPACE.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var sentence = StringBuilder()
        for (word in words) {
            val newlineWidth = font.getStringWidth(sentence.toString() + word) / 1000 * pdfConstructor.getFontSize()
            if (newlineWidth > pageWidth) {
                newLines.add(sentence.toString())
                sentence = StringBuilder((if (LIST_BEGIN == line.substring(0, 2)) "  " else NOTHING) + word)
            } else {
                sentence.append(if (StringHelper.hasText(sentence)) SPACE else NOTHING).append(word)
            }
        }
        newLines.add(sentence.toString())
    }

    private fun handleDate(str: String): String {
        var s = str
        val c = Calendar.getInstance()
        c.setTimeInMillis(System.currentTimeMillis())
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH) + 1
        val day = c.get(Calendar.DAY_OF_MONTH)
        s = s.replace("<date>", (if (day < 10) "0$day" else day).toString() + "." + (if (month < 10) "0$month" else month) + "." + year)
        return s
    }

    @Throws(IOException::class)
    private fun handleParts(pdfConstructor: PdfConstructor, str: String): String {
        var s = str
        val textParts = s.split(PART_TOKEN.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (t in 1 until textParts.size) {
            val partId = PART_TOKEN + textParts[t].substring(0, textParts[t].indexOf(BRACKET_CLOSE) + 1)
            for (part in pdfConstructor.getParts()) {
                if (part.contains(partId)) {
                    val insertedText = prepareText(part.replace(partId, NOTHING))
                    val checkForMultiline = checkStringLength(pdfConstructor, *insertedText)
                    if (checkForMultiline.size == 1) {
                        s = s.replace(partId, insertedText[0])
                    } else if (checkForMultiline.size > 1) {
                        val bold = pdfConstructor.isUseBoldFont()
                        val lines = pdfConstructor.getLines()
                        s = checkForMultiline[checkForMultiline.size - 1] // Recursive call to avoid empty space
                        pdfConstructor.setLines(Arrays.copyOfRange(checkForMultiline, 0, checkForMultiline.size - 1))
                        pdfConstructor.setInsidePart(true)
                        parseLines(pdfConstructor)
                        pdfConstructor.setInsidePart(false)
                        pdfConstructor.setLines(lines)
                        pdfConstructor.setUseBoldFont(bold)
                    }
                }
            }
        }
        return s
    }

    @Throws(IOException::class)
    private fun handlePostTitleParts(pdfConstructor: PdfConstructor, str: String): String {
        var s = str
        val partToken = "<part_"
        val textParts = s.split(partToken.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (t in 1 until textParts.size) {
            val partId = partToken + textParts[t].substring(0, textParts[t].indexOf(QUOTE))
            for (part in pdfConstructor.getParts()) {
                if (part.contains(partId)) {
                    val title = s.replace(partId + "\"", NOTHING).replace("\">", NOTHING)
                    val insertedText = prepareText(part.replace("$partId>", NOTHING) + title)
                    val checkForMultiline = checkStringLength(pdfConstructor, *insertedText)
                    if (checkForMultiline.size == 1) {
                        s = checkForMultiline[0]
                    } else if (checkForMultiline.size > 1) {
                        val bold = pdfConstructor.isUseBoldFont()
                        val lines = pdfConstructor.getLines()
                        s = checkForMultiline[checkForMultiline.size - 1] // Recursive call to avoid empty space
                        pdfConstructor.setLines(Arrays.copyOfRange(checkForMultiline, 0, checkForMultiline.size - 1))
                        parseLines(pdfConstructor)
                        pdfConstructor.setLines(lines)
                        pdfConstructor.setUseBoldFont(bold)
                    }
                }
            }
        }
        return s
    }

    @Throws(IOException::class)
    private fun handlePreTitleParts(pdfConstructor: PdfConstructor, str: String): String {
        var s = str
        val textParts = s.split(PART_TOKEN.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (t in 1 until textParts.size) {
            val partId = PART_TOKEN + textParts[t].substring(textParts[t].lastIndexOf(QUOTE) + 1, textParts[t].length)
            for (part in pdfConstructor.getParts()) {
                if (part.contains(partId)) {
                    val title = s.substring(s.indexOf(QUOTE) + 1, s.lastIndexOf(QUOTE))
                    val insertedText = prepareText(title + part.substring(part.indexOf(BRACKET_CLOSE) + 1, part.length))
                    val checkForMultiline = checkStringLength(pdfConstructor, *insertedText)
                    if (checkForMultiline.size == 1) {
                        s = checkForMultiline[0]
                    } else if (checkForMultiline.size > 1) {
                        val bold = pdfConstructor.isUseBoldFont()
                        val lines = pdfConstructor.getLines()
                        s = checkForMultiline[checkForMultiline.size - 1] // Recursive call to avoid empty space
                        pdfConstructor.setLines(Arrays.copyOfRange(checkForMultiline, 0, checkForMultiline.size - 1))
                        parseLines(pdfConstructor)
                        pdfConstructor.setLines(lines)
                        pdfConstructor.setUseBoldFont(bold)
                    }
                }
            }
        }
        return s
    }

    private fun handleSize(str: String): Float {
        val sizeToken = "<s"
        val substring = str.substring(str.lastIndexOf(sizeToken) + sizeToken.length, str.length)
        val size = substring.substring(0, substring.indexOf(BRACKET_CLOSE))
        return size.toFloat()
    }

    @Throws(IOException::class)
    private fun handleBreakBelow(pdfConstructor: PdfConstructor, str: String) {
        val substring = str.substring(str.lastIndexOf(BREAK_TOKEN) + BREAK_TOKEN.length, str.length)
        val breakMargin = substring.substring(0, substring.indexOf(BRACKET_CLOSE))
        if (pdfConstructor.getConfig().getLine() < Integer.valueOf(breakMargin)) {
            // Add new Page
            addPage(pdfConstructor)
        }
    }

    private fun addPage(pdfConstructor: PdfConstructor) {
        pdfConstructor.getConfig().setLine(pdfConstructor.getPage().mediaBox.height - pdfConstructor.getConfig().getMarginTop())
        if (pdfConstructor.getDocument().getPages().getCount() == 1) {
            drawPageNumber(pdfConstructor)
        }
        pdfConstructor.getContentStream().close()
        val nextPage = PDPage()
        pdfConstructor.getDocument().addPage(nextPage)
        pdfConstructor.setContentStream(PDPageContentStream(pdfConstructor.getDocument(), nextPage))
        drawPageNumber(pdfConstructor)
    }

    private fun handleMargin(pdfConstructor: PdfConstructor, str: String) {
        val substring = str.substring(str.lastIndexOf(MARGIN_TOKEN) + MARGIN_TOKEN.length, str.length)
        val marginString = substring.substring(0, substring.indexOf(BRACKET_CLOSE))
        val margins = marginString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (margins.size == 4) {
            pdfConstructor.getConfig().setMargins(
                    margins[0].toFloat(),
                    margins[1].toFloat(),
                    margins[2].toFloat(),
                    margins[3].toFloat())
        }
    }

    private fun handleBoldSegment(pdfConstructor: PdfConstructor, str: String) {
        val substring = str.substring(str.lastIndexOf(BOLD_TOKEN) + BOLD_TOKEN.length, str.length)
        val boldString = substring.substring(0, substring.indexOf(BRACKET_CLOSE))
        val boldSegments = boldString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (boldSegments.size == 2) {
            pdfConstructor.getConfig().setBoldSegment(Integer.valueOf(boldSegments[0]), Integer.valueOf(boldSegments[1]))
        }
    }

    @Throws(IOException::class)
    private fun handleImage(pdfConstructor: PdfConstructor, str: String) {
        val substring = str.substring(str.lastIndexOf(IMAGE_TOKEN) + IMAGE_TOKEN.length, str.length)
        val imageName = substring.substring(0, substring.indexOf(BRACKET_CLOSE))
        pdfConstructor.getConfig().setLine(drawImage(pdfConstructor, imageName))
    }

    @Throws(IOException::class)
    private fun handleTable(pdfConstructor: PdfConstructor, str: String) {
        val substring = str.substring(str.lastIndexOf(TABLE_TOKEN) + TABLE_TOKEN.length, str.length)
        val tableName = substring.substring(0, substring.indexOf(BRACKET_CLOSE))
        drawTable(pdfConstructor, tableName)
    }

    @Throws(IOException::class)
    private fun handlePageBreak(pdfConstructor: PdfConstructor) {
        addPage(pdfConstructor)
    }

    @Throws(IOException::class)
    private fun drawImage(pdfConstructor: PdfConstructor, drawableName: String): Float {
        val bitmap = BitmapFactory.decodeResource(context.resources, getDrawableIdByName(context,drawableName))
        val pdImage = LosslessFactory.createFromImage(pdfConstructor.getDocument(), bitmap)
        val trueHeight: Float = 64f
        val trueWidth: Float = 64f

        pdfConstructor.getContentStream().drawImage(pdImage, pdfConstructor.getConfig().getMarginLeft().toFloat(), pdfConstructor.getPage().mediaBox.height - trueHeight - pdfConstructor.getConfig().getMarginTop(),trueWidth,trueHeight)
        return if (pdfConstructor.getConfig().getLine() == 0f) pdfConstructor.getPage().mediaBox.height - pdfConstructor.getConfig().getMarginTop() - trueHeight else pdfConstructor.getConfig().getLine() - trueHeight
    }

    @Throws(IOException::class)
    private fun drawString(pdfConstructor: PdfConstructor): Float {
        val font = if (pdfConstructor.isUseBoldFont()) pdfConstructor.getFontBold() else pdfConstructor.getFont()

        val boldStart = pdfConstructor.getConfig().getBoldSegmentStart()
        val boldEnd = pdfConstructor.getConfig().getBoldSegmentEnd()
        var lineWidth = 0f
        var pre: String? = null
        var boldText: String? = null
        var post: String? = null
        if (boldStart >= 0 && boldEnd >= 0) {
            val tokens = pdfConstructor.getConfig().getText().split(SPACE)
            if (boldStart != 0) {
                pre = concatTokens(tokens, 0, boldStart, SPACE, false)
                lineWidth += font.getStringWidth(pre) / 1000 * pdfConstructor.getFontSize()
            }
            boldText = concatTokens(tokens, boldStart, boldEnd, SPACE, false)
            lineWidth += pdfConstructor.getFontBold().getStringWidth(boldText) / 1000 * pdfConstructor.getFontSize()
            if (boldEnd != tokens.size) {
                post = concatTokens(tokens, boldEnd, tokens.size, SPACE, true)
                lineWidth += font.getStringWidth(post) / 1000 * pdfConstructor.getFontSize()
            }
            pdfConstructor.getConfig().setBoldSegment(-1, -1)
        } else {
            pre = pdfConstructor.getConfig().getText()
            lineWidth = font.getStringWidth(pdfConstructor.getConfig().getText()) / 1000 * pdfConstructor.getFontSize()
        }
        val lineHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * pdfConstructor.getFontSize()

        pdfConstructor.getContentStream().beginText()
        pdfConstructor.getContentStream().setFont(font, pdfConstructor.getFontSize())
        var line = 0f
        if (pdfConstructor.getConfig().getDirection() === PdfLineConfiguration.Direction.FORWARD) {
            line = if (pdfConstructor.getConfig().getLine() == 0f) pdfConstructor.getPage().mediaBox.height - pdfConstructor.getConfig().getMarginTop() - lineHeight * pdfConstructor.getConfig().getLine() else pdfConstructor.getConfig().getLine() - lineHeight
        } else {
            line = if (pdfConstructor.getConfig().getLine() == 0f) pdfConstructor.getConfig().getMarginBottom() + lineHeight * pdfConstructor.getConfig().getLine() else pdfConstructor.getConfig().getLine() + lineHeight
        }
        var entryPoint = 0f
        if (pdfConstructor.getConfig().getAlignment() === PdfLineConfiguration.Alignment.CENTER) {
            entryPoint = (pdfConstructor.getPage().mediaBox.width - lineWidth) / 2
            pdfConstructor.getContentStream().newLineAtOffset(entryPoint, line)
            if (pre != null) {
                pdfConstructor.getContentStream().showText(pre)
                entryPoint += font.getStringWidth(pre) / 1000 * pdfConstructor.getFontSize()
                pdfConstructor.getContentStream().endText()
                pdfConstructor.getContentStream().beginText()
                pdfConstructor.getContentStream().newLineAtOffset(entryPoint, line)
            }
            if (boldText != null) {
                pdfConstructor.getContentStream().setFont(pdfConstructor.getFontBold(), pdfConstructor.getFontSize())
                pdfConstructor.getContentStream().showText(boldText)
                pdfConstructor.getContentStream().setFont(font, pdfConstructor.getFontSize())
                entryPoint = entryPoint + font.getStringWidth(boldText) / 1000 * pdfConstructor.getFontSize()
                pdfConstructor.getContentStream().endText()
                pdfConstructor.getContentStream().beginText()
                pdfConstructor.getContentStream().newLineAtOffset(entryPoint, line)
            }
            if (post != null) {
                pdfConstructor.getContentStream().showText(post)
            }
        } else if (pdfConstructor.getConfig().getAlignment() === PdfLineConfiguration.Alignment.LEFT || pdfConstructor.getConfig().getAlignment() === PdfLineConfiguration.Alignment.BOTTOM_LEFT) {
            entryPoint = pdfConstructor.getConfig().getMarginLeft()
            pdfConstructor.getContentStream().newLineAtOffset(pdfConstructor.getConfig().getMarginLeft(), line)
            if (pre != null) {
                pdfConstructor.getContentStream().showText(pre)
                entryPoint = entryPoint + font.getStringWidth(pre) / 1000 * pdfConstructor.getFontSize()
                pdfConstructor.getContentStream().endText()
                pdfConstructor.getContentStream().beginText()
                pdfConstructor.getContentStream().newLineAtOffset(entryPoint, line)
            }
            if (boldText != null) {
                pdfConstructor.getContentStream().setFont(pdfConstructor.getFontBold(), pdfConstructor.getFontSize())
                pdfConstructor.getContentStream().showText(boldText)
                entryPoint = entryPoint + pdfConstructor.getFontBold().getStringWidth(boldText) / 1000 * pdfConstructor.getFontSize()
                pdfConstructor.getContentStream().endText()
                pdfConstructor.getContentStream().beginText()
                pdfConstructor.getContentStream().newLineAtOffset(entryPoint, line)
                pdfConstructor.getContentStream().setFont(font, pdfConstructor.getFontSize())
            }
            if (post != null) {
                pdfConstructor.getContentStream().showText(post)
            }
        } else if (pdfConstructor.getConfig().getAlignment() === PdfLineConfiguration.Alignment.RIGHT || pdfConstructor.getConfig().getAlignment() === PdfLineConfiguration.Alignment.BOTTOM_RIGHT) {
            entryPoint = pdfConstructor.getPage().mediaBox.width - lineWidth - pdfConstructor.getConfig().getMarginRight()
            pdfConstructor.getContentStream().newLineAtOffset(entryPoint, line)
            if (pre != null) {
                pdfConstructor.getContentStream().showText(pre)
                entryPoint = entryPoint + font.getStringWidth(pre) / 1000 * pdfConstructor.getFontSize()
                pdfConstructor.getContentStream().endText()
                pdfConstructor.getContentStream().beginText()
                pdfConstructor.getContentStream().newLineAtOffset(entryPoint, line)
            }
            if (boldText != null) {
                pdfConstructor.getContentStream().setFont(pdfConstructor.getFontBold(), pdfConstructor.getFontSize())
                pdfConstructor.getContentStream().showText(boldText)
                pdfConstructor.getContentStream().setFont(font, pdfConstructor.getFontSize())
                entryPoint = entryPoint + font.getStringWidth(boldText) / 1000 * pdfConstructor.getFontSize()
                pdfConstructor.getContentStream().endText()
                pdfConstructor.getContentStream().beginText()
                pdfConstructor.getContentStream().newLineAtOffset(entryPoint, line)
            }
            if (post != null) {
                pdfConstructor.getContentStream().showText(post)
            }
        }
        pdfConstructor.getContentStream().endText()
        return line
    }

    @Throws(IOException::class)
    private fun drawTable(pdfConstructor: PdfConstructor, tableName: String) {
        val contentStream = pdfConstructor.getContentStream()
        val content = pdfConstructor.getTableData(tableName)
        if (content != null && content.size > 0 && content[0].size > 0) {
            val linePointer = if (pdfConstructor.getConfig().getLine() == 0f) pdfConstructor.getConfig().getMarginTop() else pdfConstructor.getConfig().getLine()
            val font = if (pdfConstructor.isUseBoldFont()) pdfConstructor.getFontBold() else pdfConstructor.getFont()
            val columnLengthList = ArrayList<Float>()
            val textLengthList = ArrayList<Float>()

            val oversizeWidth = 1.1f
            val oversizeHeight = 1.2f

            val rowCount = content.size
            val colCount = content[0].size
            val multiLines = HashMap<Int,Int>()
            val emptyLines = HashMap<Int,Boolean>()
            val rowHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * pdfConstructor.getFontSize() * oversizeHeight
            val rowHeightText = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * pdfConstructor.getFontSize()

            // calculations
            var completeLength = pdfConstructor.getConfig().getMarginLeft()
            var multiline = 1
            for (column in 0 until content[0].size) {
                var length = 0f
                for (rows in content.indices) {
                    val entries = ObjectHelper.nvl(content[rows][column], NOTHING).split(NEW_LINE)
                    multiline = 1
                    for (entry in entries) {
                        if (column == 0) {
                            if (StringHelper.hasText(entry)) {
                                emptyLines[rows] = false
                            } else if (rows != 0) {
                                emptyLines[rows] = true
                            }
                        }
                        for (cutEntry in checkStringLengthForTable(pdfConstructor,entry, textLengthList)){
                            multiline++
                            val lineWidth = pdfConstructor.getFontBold().getStringWidth(cutEntry) / 1000 * pdfConstructor.getFontSize()
                            length = if (length < lineWidth) lineWidth else length
                        }
                    }
                    multiLines[rows] = multiline
                }
                textLengthList.add(length)
                length *= oversizeWidth
                completeLength += length
                columnLengthList.add(length)
            }
            val colWidths = columnLengthList.toTypedArray()
            val textWidths = textLengthList.toTypedArray()
            // draw the rows
            var nextY = linePointer
            val emptyCoordinates = ArrayList<Pair<Float,Float>>()
            var contentPointer = 0
            var linesAfterSpace = 0 //Up to 3 lines should be pushed to the next page
            var yBeforeEmptyLine = 0f
            var pointerOfSpace = 0
            val postponedLines = ArrayList<Float>()
            var previousY = nextY
            for (i in NumberHelper.nvl(pdfConstructor.getTablePointer(tableName),0) .. rowCount) {
                previousY = nextY
                if (emptyCoordinates.isNotEmpty()){
                    postponedLines.add(nextY)
                }else{
                    drawLine(contentStream, pdfConstructor.getConfig().getMarginLeft(), nextY, completeLength, nextY)
                }
                if (i == pdfConstructor.getTablePointer(tableName)) {
                    nextY -= rowHeight
                } else {
                    nextY -= rowHeight * NumberHelper.nvl(multiLines[i],1)
                }
                if (emptyCoordinates.isNotEmpty()){
                    linesAfterSpace++
                }
                val emptyLine = ObjectHelper.nvl(emptyLines[i],false)
                if (emptyLine){
                    pointerOfSpace = i
                    emptyCoordinates.add(Pair(previousY,nextY))
                    yBeforeEmptyLine = previousY
                }
                if (nextY < pdfConstructor.getConfig().getMarginBottom() && i != rowCount) {
                    contentPointer = i - 1
                    nextY = previousY
                    if (emptyLine){
                        emptyCoordinates.removeAt(emptyCoordinates.size-1)
                    }
                    break
                }
            }
            if (linesAfterSpace <= 3 && emptyCoordinates.isNotEmpty()){ //Rollback if only small amount of Lines would come after Space
                emptyCoordinates.removeAt(emptyCoordinates.size-1)
                nextY = yBeforeEmptyLine
                contentPointer -= linesAfterSpace
                content.removeAt(pointerOfSpace)
                pdfConstructor.addTableData(tableName,content)
            }else{
                var endOfLastSpace = 0f
                if (emptyCoordinates.isNotEmpty()){
                    endOfLastSpace = emptyCoordinates.get(emptyCoordinates.lastIndex).second
                }
                for (y in postponedLines){
                    if (y != endOfLastSpace){
                        drawLine(contentStream, pdfConstructor.getConfig().getMarginLeft(), y, completeLength, y)
                    }
                }
            }
            // draw the columns
            var nextX = pdfConstructor.getConfig().getMarginLeft()
            if (emptyCoordinates.isEmpty()){
                drawLine(contentStream, nextX, linePointer, nextX, nextY + (if (contentPointer == 0) rowHeight else 0f))
                for (i in 0 until colCount) {
                    nextX += colWidths[i]
                    drawLine(contentStream, nextX, linePointer, nextX, nextY + (if (contentPointer == 0) rowHeight else 0f))
                }
            }else{
                var tempY = linePointer
                val tempX = nextX
                for (entry in emptyCoordinates){
                    drawLine(contentStream, nextX, tempY, nextX, entry.first)
                    for (i in 0 until colCount) {
                        nextX += colWidths[i]
                        drawLine(contentStream, nextX, tempY, nextX, entry.first)
                    }
                    tempY = entry.second + rowHeight
                    if (tempY - nextY > rowHeight*1.2){
                        drawLine(contentStream, pdfConstructor.getConfig().getMarginLeft(), tempY, completeLength, tempY)
                        drawLine(contentStream, pdfConstructor.getConfig().getMarginLeft(), tempY-rowHeight, completeLength, tempY-rowHeight)
                    }
                    nextX = tempX
                }
                if (tempY - nextY > rowHeight*1.2) {
                    drawLine(contentStream, nextX, tempY, nextX, nextY + (if (contentPointer == 0) rowHeight else 0f))
                    for (i in 0 until colCount) {
                        nextX += colWidths[i]
                        drawLine(contentStream, nextX, tempY, nextX, nextY + (if (contentPointer == 0) rowHeight else 0f))
                    }
                }
            }
            var textX = pdfConstructor.getConfig().getMarginLeft()
            var textY = linePointer - rowHeightText
            // draw the text
            val pointerList = HashMap<Int,Float>()
            for (column in 0 until content[0].size) {
                val textWidth = textWidths[column]
                val fraction = (oversizeWidth - 1f) / 2f
                textX += textWidth * fraction
                val textYCache = textY
                for (rows in NumberHelper.nvl(pdfConstructor.getTablePointer(tableName),0) until if (contentPointer == 0) rowCount else contentPointer + 1) {
                    var entry = ObjectHelper.nvl(content[rows][column], NOTHING)
                    if (rows == pdfConstructor.getTablePointer(tableName) && pdfConstructor.getTablePointer(tableName) != 0) {
                        entry = ObjectHelper.nvl(content[0][column], NOTHING)
                    }
                    val entries = entry.split(NEW_LINE.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    val prePostSpace: Float = (rowHeight-rowHeightText)/2 //if (multiLines[rows] == entries.size || rows == pdfConstructor.getTablePointer(tableName)) 0f else rowHeight * (multiLines[rows]!!) / 2
                    textY -= prePostSpace
                    if (column == 0){
                        pointerList[rows] = textY //Save the Pointer for other Columns
                    }else{
                        val newY = pointerList[rows] //Align to first Column
                        if (newY != null){
                            textY = newY
                        }
                    }
                    for (e in entries) {
                        val lengthSplitString = checkStringLengthForTable(pdfConstructor, e, textLengthList.subList(0, column))
                        for (cutEntryPos in lengthSplitString.indices) {
                            contentStream.beginText()
                            contentStream.newLineAtOffset(textX, textY)
                            contentStream.setFont(if (rows == pdfConstructor.getTablePointer(tableName)) pdfConstructor.getFontBold() else pdfConstructor.getFont(), pdfConstructor.getFontSize())
                            contentStream.showText(lengthSplitString[cutEntryPos])
                            contentStream.endText()
                            textY -= if (entry.contains(NEW_LINE) || rows == pdfConstructor.getTablePointer(tableName) || (lengthSplitString.size > 1 && cutEntryPos < lengthSplitString.size-1)) rowHeight else rowHeight * multiLines[rows]!! - 2 * prePostSpace
                        }
                    }
                    textY -= prePostSpace
                    if (textY < pdfConstructor.getConfig().getMarginBottom()) {
                        break
                    }
                }
                textY = textYCache
                textX += textWidth * (oversizeWidth - fraction)
            }
            // if pagebreak, append new page
            if (contentPointer != 0) {
                pdfConstructor.setTablePointer(tableName, if (contentPointer < 0) 0 else contentPointer)
                pdfConstructor.getConfig().setLine(pdfConstructor.getPage().mediaBox.height - pdfConstructor.getConfig().getMarginTop())
                if (pdfConstructor.getDocument().pages.count == 1) {
                    drawPageNumber(pdfConstructor)
                }
                contentStream.close()
                val nextPage = PDPage()
                pdfConstructor.getDocument().addPage(nextPage)
                pdfConstructor.setContentStream(PDPageContentStream(pdfConstructor.getDocument(), nextPage))
                drawPageNumber(pdfConstructor)
                drawTable(pdfConstructor, tableName)
            } else {
                pdfConstructor.getConfig().setLine(nextY)
            }
        }
    }

    private fun concatTokens(tokens: List<String>, start: Int, end: Int, delimiter: String, last: Boolean): String {
        val sb = StringBuilder()
        for (i in start until if (end < tokens.size) end else tokens.size) {
            sb.append(tokens[i]).append(delimiter)
        }
        return if (last) sb.substring(0, sb.length - delimiter.length) else sb.toString()
    }

    @Throws(IOException::class)
    private fun drawLine(contentStream: PDPageContentStream, xStart: Float, yStart: Float, xEnd: Float, yEnd: Float) {
        contentStream.moveTo(xStart, yStart)
        contentStream.lineTo(xEnd, yEnd)
        contentStream.stroke()
    }

    private fun createTable(columnHeaders: Array<String>, rowHeaders: Array<String>): Array<Array<String>> {
        val tableContent = Array<Array<String?>>(rowHeaders.size) { arrayOfNulls(columnHeaders.size) }
        for (column in columnHeaders.indices) {
            tableContent[0][column] = columnHeaders[column]
        }
        for (row in rowHeaders.indices) {
            tableContent[row][0] = rowHeaders[row]
        }
        return tableContent as Array<Array<String>>
    }

    private fun fillTableColumnContent(table: Array<Array<String>>, values: Array<String>, columnId: Int, offset: Int) {
        for (c in offset until if (offset + values.size < table.size) offset + values.size else table.size) {
            table[c][columnId] = values[c - offset]
        }
    }

    private fun fillTableRowContent(table: Array<Array<String>>, values: Array<String>, rowId: Int, offset: Int) {
        for (r in offset until if (values.size < table.size) values.size else table.size) {
            table[rowId][r] = values[r - offset]
        }
    }

    private fun createNumberedRows(numbers: Int, headerSpacer: Boolean): Array<String> {
        val numberedRows = arrayOfNulls<String>(numbers + if (headerSpacer) 1 else 0)
        for (i in 1 until numberedRows.size) {
            numberedRows[if (headerSpacer) i else i - 1] = SPACE + (if (i < 10) SPACE else NOTHING) + i + SPACE
        }
        return numberedRows as Array<String>
    }

}