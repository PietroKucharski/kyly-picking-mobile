package com.kyly.picking.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AddressChip(address: String, onClick: () -> Unit = {}) {
    AssistChip(
        onClick = onClick,
        label   = {
            Text(
                text  = address,
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}
