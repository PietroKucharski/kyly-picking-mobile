package com.kyly.picking.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kyly.picking.data.remote.dto.PostBipagemRequest

@Entity(tableName = "bipagens_pendentes")
data class BipagemPendenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemCaixaId:     String,
    val codigoSkuBipado: String,
    val enderecoId:      String,
    val quantidade:      Int,
    val statusColeta:    String,
    val criadoEm:        Long = System.currentTimeMillis(),
)

fun BipagemPendenteEntity.toRequest() = PostBipagemRequest(
    itemCaixaId     = itemCaixaId,
    codigoSkuBipado = codigoSkuBipado,
    enderecoId      = enderecoId,
    quantidade      = quantidade,
    statusColeta    = statusColeta,
)
