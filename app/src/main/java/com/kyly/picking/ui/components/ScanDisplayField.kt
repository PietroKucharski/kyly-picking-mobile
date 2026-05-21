package com.kyly.picking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyly.picking.ui.theme.KylyPickingTheme
import com.kyly.picking.ui.theme.Outline
import com.kyly.picking.ui.theme.OutlineVariant
import com.kyly.picking.ui.theme.Primary
import com.kyly.picking.ui.theme.Secondary
import com.kyly.picking.ui.theme.SurfaceContainerLow

@Composable
fun ScanDisplayField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    val shape = RoundedCornerShape(4.dp)
    val borderColor = when {
        value.isNotBlank() -> Primary
        isActive           -> Secondary
        else               -> OutlineVariant
    }
    val borderWidth = if (isActive || value.isNotBlank()) 2.dp else 1.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(SurfaceContainerLow)
            .border(borderWidth, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isBlank()) {
            Text(
                text  = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = Outline,
            )
        } else {
            Text(
                text  = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanDisplayFieldEmptyPreview() {
    KylyPickingTheme {
        ScanDisplayField(
            value       = "",
            placeholder = "Aguardando bipagem...",
            isActive    = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanDisplayFieldFilledPreview() {
    KylyPickingTheme {
        ScanDisplayField(
            value       = "SUP001",
            placeholder = "Aguardando bipagem...",
        )
    }
}
