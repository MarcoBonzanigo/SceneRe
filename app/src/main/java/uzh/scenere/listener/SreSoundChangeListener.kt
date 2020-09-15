package uzh.scenere.listener

import android.app.Activity
import android.media.MediaRecorder
import uzh.scenere.R
import uzh.scenere.const.Constants.Companion.HALF_SEC_MS
import uzh.scenere.datastructures.StatisticArrayList
import uzh.scenere.helpers.NumberHelper
import uzh.scenere.helpers.PermissionHelper
import java.io.IOException
import java.util.logging.Handler

class SreSoundChangeListener(activity: Activity) {

    private var recorder: MediaRecorder? = null
    var audioPossible = true
    var audioAllowed = true
    var calibrationReference: Double = 0.0
    var calibrating: Boolean = false
    private val sampleSize = 100

    init {
        audioAllowed = PermissionHelper.check(activity, PermissionHelper.Companion.PermissionGroups.AUDIO)
        if (audioAllowed) {
            recorder = MediaRecorder()
            recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder?.setOutputFile("/dev/null")
            try {
                recorder?.prepare()
            } catch (e: IllegalStateException) {
                audioPossible = false
            } catch (e: IOException) {
                audioPossible = false
            }
        }
    }

    fun start(): SreSoundChangeListener{
        if (audioPossible && audioAllowed){
            recorder?.start()
            calibrate()
        }
        return this
    }

    fun stop(): SreSoundChangeListener{
        recorder?.stop()
        recorder?.release()
        recorder = null
        return this
    }

    fun calibrate(): SreSoundChangeListener{
        calibrating = true
        android.os.Handler().postDelayed({
            val list = StatisticArrayList<Int>()
            while (list.size < sampleSize){
                val amplitude = getAmplitudeInternal()
                if (amplitude != 0){
                    list.add(amplitude)
                }
            }
            val nvl = NumberHelper.nvl(list.avgLarge(), 1)
            calibrationReference = NumberHelper.capAtLow(nvl,1).toDouble()
            calibrating = false
        },HALF_SEC_MS)
        return this
    }

    private fun getAmplitudeInternal(): Int {
        return NumberHelper.nvl(recorder?.maxAmplitude,0)
    }

    fun getAmplitude(): Double {
        if (calibrating || calibrationReference == 0.0){
            return 0.0
        }
        val flooredAmplitude = NumberHelper.floor((20 * Math.log10(getAmplitudeInternal() / calibrationReference)), 2)
        return if (flooredAmplitude.isInfinite() || flooredAmplitude == 0.0) 0.5 else flooredAmplitude
    }
}