package com.kyly.picking.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyly.picking.ui.theme.SuccessIndustrial
import com.kyly.picking.ui.theme.WarningIndustrial

@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor, label) = when (status) {
        "pendente" -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "PENDENTE",
        )
        "parcial"  -> Triple(
            WarningIndustrial.copy(alpha = 0.15f),
            WarningIndustrial,
            "PARCIAL",
        )
        "completo" -> Triple(
            SuccessIndustrial.copy(alpha = 0.15f),
            SuccessIndustrial,
            "COMPLETO",
        )
        "falta"    -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "FALTA",
        )
        else       -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
            status.uppercase(),
        )
    }

    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(4.dp),
        color    = bgColor,
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.5.sp),
            color    = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
