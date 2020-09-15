package uzh.scenere.datamodel.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.helpers.NumberHelper
import uzh.scenere.helpers.addOne
import java.io.ByteArrayOutputStream
import java.io.IOException


class PdfConstructor private constructor(){
    private var config: PdfLineConfiguration? = null
    private var document: PDDocument? = null
    private var page: PDPage? = null
    private var contentStream: PDPageContentStream? = null
    private var font: PDType0Font? = null
    private var fontBold: PDType0Font? = null
    private var useBoldFont: Boolean = false
    private var fontSize: Float = 0f
    private var defaultFontSize: Float = 0f
    private var lines: Array<String> = arrayOf(NOTHING)
    private var parts: Array<String> = arrayOf(NOTHING)
    private val tableData:HashMap<String, ArrayList<Array<String>>> = HashMap()
    private val tablePointer: HashMap<String, Int> = HashMap()
    private val appendedPagesForTable: HashMap<String, Int> = HashMap()
    private var insidePart = false

    constructor(config: PdfLineConfiguration, document: PDDocument, page: PDPage, contentStream: PDPageContentStream, font: PDType0Font, fontBold: PDType0Font, fontSize: Float): this() {
        this.config = config
        this.document = document
        this.page = page
        this.contentStream = contentStream
        this.font = font
        this.fontBold = fontBold
        this.fontSize = fontSize
        this.defaultFontSize = fontSize
    }

    fun getConfig(): PdfLineConfiguration {
        return config!!
    }

    fun setConfig(config: PdfLineConfiguration) {
        this.config = config
    }

    fun getDocument(): PDDocument {
        return document!!
    }

    fun setDocument(document: PDDocument) {
        this.document = document
    }

    fun getPage(): PDPage {
        return page!!
    }

    fun setPage(page: PDPage) {
        this.page = page
    }

    fun getContentStream(): PDPageContentStream {
        return contentStream!!
    }

    fun setContentStream(contentStream: PDPageContentStream) {
        this.contentStream = contentStream
    }

    fun getFont(): PDType0Font {
        return font!!
    }

    fun setFont(font: PDType0Font) {
        if (!insidePart) {
            this.font = font
        }
    }

    fun getFontBold(): PDType0Font {
        return fontBold!!
    }

    fun setFontBold(fontBold: PDType0Font) {
        this.fontBold = fontBold
    }

    fun getFontSize(): Float {
        return fontSize
    }

    fun getDefaultFontSize(): Float {
        return defaultFontSize
    }

    fun setFontSize(fontSize: Float) {
        if (!insidePart) {
            this.fontSize = fontSize
        }
    }

    fun getLines(): Array<String> {
        return lines
    }

    fun setLines(lines: Array<String>) {
        this.lines = lines
    }

    fun getParts(): Array<String> {
        return parts
    }

    fun setParts(parts: Array<String>) {
        this.parts = parts
    }

    fun isUseBoldFont(): Boolean {
        return useBoldFont
    }

    fun setUseBoldFont(useBoldFont: Boolean) {
        this.useBoldFont = useBoldFont
    }

    fun getTableData(identifier: String): ArrayList<Array<String>>? {
        return tableData[identifier]
    }

    fun getTablePointer(identifier: String): Int? {
        return tablePointer[identifier]
    }

    fun setTablePointer(identifier: String, pointer: Int) {
        this.tablePointer[identifier] = pointer
        this.appendedPagesForTable.addOne(identifier)
    }

    fun getAppendedPagesForTable(identifier: String): Int{
        return NumberHelper.nvl(appendedPagesForTable[identifier],0)
    }

    fun addTableData(identifier: String, tableData: ArrayList<Array<String>>) {
        this.tableData[identifier] = tableData
        this.tablePointer[identifier] = 0
    }

    @Throws(IOException::class)
    fun closeContentStream() {
        this.contentStream!!.close()
    }

    @Throws(IOException::class)
    fun getBytes(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        this.document!!.save(byteArrayOutputStream)
        val content = byteArrayOutputStream.toByteArray()
        byteArrayOutputStream.close()
        return content
    }

    @Throws(IOException::class)
    fun getBDocument(): PDDocument? {
        return this.document
    }

    fun revertToDefaultFontSize() {
        this.fontSize = defaultFontSize
    }

    fun isInsidePart(): Boolean {
        return insidePart
    }

    fun setInsidePart(insidePart: Boolean) {
        this.insidePart = insidePart
    }

}