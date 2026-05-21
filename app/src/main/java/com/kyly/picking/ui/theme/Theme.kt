package com.kyly.picking.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KylyColorScheme = lightColorScheme(
    primary                 = Primary,
    onPrimary               = OnPrimary,
    primaryContainer        = PrimaryContainer,
    onPrimaryContainer      = OnPrimaryContainer,
    secondary               = Secondary,
    onSecondary             = OnSecondary,
    secondaryContainer      = SecondaryContainer,
    onSecondaryContainer    = OnSecondaryContainer,
    tertiaryContainer       = TertiaryContainer,
    onTertiaryContainer     = OnTertiaryContainer,
    background              = Background,
    onBackground            = OnBackground,
    surface                 = Surface,
    onSurface               = OnSurface,
    surfaceVariant          = SurfaceContainerHigh,
    onSurfaceVariant        = OnSurfaceVariant,
    outline                 = Outline,
    outlineVariant          = OutlineVariant,
    error                   = KylyError,
    onError                 = OnKylyError,
    errorContainer          = ErrorContainer,
    onErrorContainer        = OnErrorContainer,
    surfaceContainerLowest  = SurfaceContainerLowest,
    surfaceContainerLow     = SurfaceContainerLow,
    surfaceContainer        = SurfaceContainer,
    surfaceContainerHigh    = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

@Composable
fun KylyPickingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KylyColorScheme,
        typography  = KylyTypography,
        content     = content,
    )
}
