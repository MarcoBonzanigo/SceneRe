package uzh.scenere.datamodel.pdf

import android.content.Context
import uzh.scenere.const.Constants.Companion.ANALYTICS_EXPORT_NAME
import uzh.scenere.const.Constants.Companion.FOLDER_ANALYTICS
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.helpers.FileHelper
import uzh.scenere.helpers.StringHelper
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Exception
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class PdfFileBean {
    private val serialVersionUID = 1L

    enum class FileType constructor(private val type: String) {
        PDF(".pdf"), ZIP(".zip");

        override fun toString(): String {
            return type
        }
    }

    var finished = false
    private var content: ByteArray? = null
    private var type: String? = null
    private val fileContents: ArrayList<ByteArray> = ArrayList()
    private val fileNames: ArrayList<String> = ArrayList()

    fun getContent(): ByteArray? {
        return content
    }

    fun getType(): String {
        return type!!
    }

    fun addFile(content: ByteArray, name: String, randomizeName: Boolean) {
        this.fileContents.add(content)
        this.fileNames.add(if (randomizeName) name + "_" + System.currentTimeMillis().toString() else name)
    }

    fun finish() {
        if (fileContents.size == 1) { // Single File
            content = fileContents[0]
            type = FileType.PDF.toString()
        } else { // Multiple Files
            try {
                val baos = ByteArrayOutputStream()
                val zos = ZipOutputStream(baos)
                for (file in 0 until fileContents.size) {
                    val entry = ZipEntry(fileNames[file].plus(FileType.PDF.toString()))
                    entry.size = fileContents[file].size.toLong()
                    zos.putNextEntry(entry)
                    zos.write(fileContents[file])
                    zos.closeEntry()
                }
                zos.close()
                content = baos.toByteArray()
                type = FileType.ZIP.toString()
            } catch (e: IOException) {
                return
            }

        }
        finished = true
    }

    fun isValid(): Boolean {
        return finished && content!!.isNotEmpty()
    }

    fun write(context: Context): String {
        var path: String? = null
        try{
            if (content != null){
                path = FileHelper.writeFile(context,content!!,ANALYTICS_EXPORT_NAME+System.currentTimeMillis()+getType(), FOLDER_ANALYTICS)
            }
        }catch(e: Exception){
            return NOTHING
        }
        return StringHelper.nvl(path,NOTHING)
    }
}