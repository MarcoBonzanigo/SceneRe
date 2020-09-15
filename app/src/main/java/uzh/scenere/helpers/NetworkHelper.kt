package uzh.scenere.helpers

import android.os.AsyncTask
import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.URL

class NetworkHelper {

    companion object {
        fun getBytesFromUrl(url: String): ByteArray{
            return NetworkTask().execute(url).get()
        }

        private fun getByteArrayImageFromUrl(url: String): ByteArray {
            try {
                val imageUrl = URL(url)
                val connection = imageUrl.openConnection()
                val inputStream = connection.getInputStream()
                val bufferedInputStream = BufferedInputStream(inputStream)
                val buffer = ByteArrayOutputStream()
                val cache = ByteArray(500)
                var current = 0
                do {
                    current = bufferedInputStream.read(cache,0,cache.size)
                    if (current != -1){
                        buffer.write(cache,0,current)
                    }
                }while(current != -1)
                return buffer.toByteArray()
            } catch (e: Exception) {
                Log.d(this.className(), "Error: " + e.toString())
            }

            return ByteArray(0)
        }
    }

    class NetworkTask() : AsyncTask<String, Void, ByteArray>() {
        override fun doInBackground(vararg params: String?): ByteArray? {
            if (params.size == 1 && params[0] != null){
                return getByteArrayImageFromUrl(params[0]!!)
            }
            return ByteArray(0)
        }


        override fun onPreExecute() {
            //NOP
        }

        override fun onPostExecute(result: ByteArray?) {
            //NOP
        }
    }
}