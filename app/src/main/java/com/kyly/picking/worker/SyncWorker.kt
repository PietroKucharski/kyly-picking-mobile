package com.kyly.picking.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kyly.picking.data.local.BipagemPendenteDao
import com.kyly.picking.data.local.toRequest
import com.kyly.picking.data.remote.ApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val bipagemDao: BipagemPendenteDao,
    private val apiService: ApiService,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pendentes = bipagemDao.listarTodas()
        if (pendentes.isEmpty()) return Result.success()

        return try {
            for (bipagem in pendentes) {
                apiService.postBipagem(bipagem.toRequest())
                bipagemDao.deletar(bipagem)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
