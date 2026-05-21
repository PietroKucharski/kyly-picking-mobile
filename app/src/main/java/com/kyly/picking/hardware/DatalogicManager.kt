package com.kyly.picking.hardware

import com.datalogic.decode.BarcodeManager
import com.datalogic.decode.DecodeException
import com.datalogic.decode.ReadListener
import com.datalogic.device.notification.Led
import com.datalogic.device.notification.LedIntensity
import com.datalogic.device.notification.NotificationManager
import com.datalogic.device.notification.Tone
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatalogicManager @Inject constructor() {

    private val _barcodeEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val barcodeEvents: SharedFlow<String> = _barcodeEvents

    private var barcodeManager: BarcodeManager? = null
    private var notificationManager: NotificationManager? = null

    private val listener = ReadListener { decodeResult ->
        _barcodeEvents.tryEmit(decodeResult.getText())
    }

    fun enable() {
        try {
            barcodeManager = BarcodeManager()
            barcodeManager?.addReadListener(listener)
            notificationManager = NotificationManager()
        } catch (e: DecodeException) {
            e.printStackTrace()
        }
    }

    fun disable() {
        try {
            barcodeManager?.removeReadListener(listener)
            barcodeManager = null
        } catch (e: DecodeException) { /* ignorar */ }
    }

    fun setLedGreen()  = setLed(Led.GREEN, LedIntensity.HIGH)
    fun setLedRed()    = setLed(Led.RED, LedIntensity.HIGH)
    fun setLedYellow() = setLed(Led.ORANGE, LedIntensity.HIGH)
    fun clearLed()     { notificationManager?.setLed(Led.GREEN, LedIntensity.OFF, 0) }

    private fun setLed(color: Led, intensity: LedIntensity) {
        notificationManager?.setLed(color, intensity, 500)
    }

    fun beepShort()             = notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 100, 100)
    fun beepDoubleShort()       {
        repeat(2) { notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 100, 100) }
    }
    fun beepContinuous(ms: Int) = notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 80, ms)
    fun beepSuccess()           = notificationManager?.setTone(Tone.BEEP_HIGH_FREQ, 80, 500)
}
