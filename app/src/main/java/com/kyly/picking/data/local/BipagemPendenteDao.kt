package com.kyly.picking.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BipagemPendenteDao {

    @Query("SELECT * FROM bipagens_pendentes ORDER BY criadoEm ASC")
    suspend fun listarTodas(): List<BipagemPendenteEntity>

    @Insert
    suspend fun inserir(bipagem: BipagemPendenteEntity)

    @Delete
    suspend fun deletar(bipagem: BipagemPendenteEntity)
}
