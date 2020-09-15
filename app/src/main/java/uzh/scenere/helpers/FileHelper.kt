package uzh.scenere.helpers

import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import android.content.Intent
import android.net.Uri
import android.os.Environment
import uzh.scenere.const.Constants.Companion.NOTHING
import java.lang.Exception
import android.os.Environment.getExternalStorageDirectory
import uzh.scenere.const.Constants.Companion.FOLDER_ANALYTICS
import uzh.scenere.const.Constants.Companion.FOLDER_DATABASE
import uzh.scenere.const.Constants.Companion.FOLDER_EXPORT
import uzh.scenere.const.Constants.Companion.FOLDER_IMPORT
import uzh.scenere.const.Constants.Companion.FOLDER_ROOT
import uzh.scenere.const.Constants.Companion.FOLDER_TEMP
import java.io.BufferedOutputStream
import java.io.FileOutputStream


class FileHelper {

    companion object {

        private fun writeFileInternal(context: Context, array: ByteArray, fileName: String): String{
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(array)
            }
            return context.filesDir.path.plus("/$fileName")
        }

        @SuppressLint("SetWorldReadable")
        private fun writeFileExternal(path: String, fileName: String, array: ByteArray, folder: String): String {
            var pathFinal = path
            if (!File(pathFinal).exists()) {
                File(pathFinal).mkdir()
            }
            pathFinal += folder
            if (!File(pathFinal).exists()) {
                File(pathFinal).mkdir()
            }
            pathFinal = pathFinal.plus("/$fileName")
            File(pathFinal).createNewFile()
            File(pathFinal).setReadable(true, false)
            val bos = BufferedOutputStream(FileOutputStream(pathFinal))
            bos.write(array)
            bos.flush()
            bos.close()
            return pathFinal
        }

        fun writeFile(context: Context, array: ByteArray, fileName: String, folder: String = FOLDER_EXPORT): String{
            var available = false
            var writeable = false
            val path = getExternalStorageDirectory().path.plus(FOLDER_ROOT)
            val state = Environment.getExternalStorageState()
            when (state) {
                Environment.MEDIA_MOUNTED -> {
                    writeable = true
                    available = true
                }
                Environment.MEDIA_MOUNTED_READ_ONLY -> {
                    available = true
                    writeable = false
                }
                else -> {
                    writeable = false
                    available = false
                }
            }
            return if (available && writeable){
                writeFileExternal(path, fileName, array, folder)
            }else{
                writeFileInternal(context,array,fileName)
            }
        }

        fun getDefaultFilePath(context: Context): String{
            return context.filesDir.path
        }

        fun readFile(context: Context, filePath: String): ByteArray{
            var bytes: ByteArray? = null
            try{
                val file = File(filePath)
                if (file.exists()){
                    bytes =  file.readBytes()
                }
            }catch (e: Exception){

            }
            return bytes ?: ByteArray(0)
        }

        fun openFolder(context: Context, uri: String): Boolean{
            val selectedUri = Uri.parse(uri)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(selectedUri, "resource/folder")
            if (intent.resolveActivityInfo(context.packageManager, 0) != null){
                context.startActivity(intent)
                return true
            }
            return false
        }

        fun removeFileFromPath(filePath: String?): String {
            if (!StringHelper.hasText(filePath)){
                return NOTHING
            }
            val split = filePath!!.split("/")
            return filePath.replace("/".plus(split[split.size-1]),NOTHING)
        }

        fun openFile(context: Context, filePath: String){
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = Uri.parse(filePath)
            intent.setDataAndType(uri, "*/*")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }

        fun sendFileViaEmail(context: Context, emailTo: String, emailCC: String, subject: String, emailText: String, filePath: String){
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.type = "text/plain"
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(emailTo))
            emailIntent.putExtra(android.content.Intent.EXTRA_CC,arrayOf(emailCC))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            emailIntent.putExtra(Intent.EXTRA_TEXT, emailText)
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath))
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(emailIntent, "Send as Mail..."))
        }

        fun getFilesInFolder(folderPath: String, fileType: String): Array<out File> {
            val folder = File(folderPath)
            val list = ArrayList<File>()
            if (folder.isDirectory){
                for (file in folder.listFiles()){
                    if (file.isFileType(fileType)){
                        list.add(file)
                    }
                }
                return list.toTypedArray()
            }
            return emptyArray()
        }

        fun checkAndCreateFolderStructure() {
            val rootPath = getExternalStorageDirectory().path.plus(FOLDER_ROOT)
            val rootFile = File(rootPath)
            if (!rootFile.exists()){
                rootFile.mkdirs()
            }
            for (folder in arrayOf(FOLDER_EXPORT, FOLDER_IMPORT, FOLDER_TEMP, FOLDER_DATABASE, FOLDER_ANALYTICS)){
                val folderFile = File(rootPath.plus(folder))
                if (!folderFile.exists()){
                    folderFile.mkdirs()
                }
            }

        }
    }
}