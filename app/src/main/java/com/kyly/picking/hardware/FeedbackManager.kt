package com.kyly.picking.hardware

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackManager @Inject constructor(
    private val datalogic: DatalogicManager,
) {
    fun trigger(event: FeedbackEvent) {
        datalogic.clearLed()
        when (event) {
            FeedbackEvent.PECA_VALIDA      -> { datalogic.setLedGreen();  datalogic.beepShort() }
            FeedbackEvent.SKU_COMPLETO     -> { datalogic.setLedGreen();  datalogic.beepDoubleShort() }
            FeedbackEvent.ERRO_SKU,
            FeedbackEvent.SEM_SALDO        -> { datalogic.setLedRed();    datalogic.beepContinuous(2000) }
            FeedbackEvent.CAIXA_FINALIZADA -> { datalogic.setLedGreen();  datalogic.beepSuccess() }
            FeedbackEvent.PICKING_PARCIAL  -> { datalogic.setLedYellow(); datalogic.beepDoubleShort() }
        }
    }
}
