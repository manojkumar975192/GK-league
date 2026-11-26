package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ElectricCobalt,
    onPrimary = OnElectricCobalt,
    primaryContainer = ElectricCobaltContainer,
    onPrimaryContainer = OnElectricCobaltContainer,
    secondary = TrophyAmber,
    onSecondary = OnTrophyAmber,
    secondaryContainer = TrophyAmberContainer,
    onSecondaryContainer = OnTrophyAmberContainer,
    tertiary = VerifiedEmerald,
    onTertiary = OnVerifiedEmerald,
    tertiaryContainer = VerifiedEmeraldContainer,
    onTertiaryContainer = OnVerifiedEmeraldContainer,
    background = BackgroundLight,
    onBackground = InkDark,
    surface = SurfaceLight,
    onSurface = InkDark,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = InkMutedLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorCrimson,
    onError = OnElectricCobalt,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CobaltLight,
    onPrimary = OnCobaltLight,
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = OnCobaltContainerDark,
    secondary = GoldLight,
    onSecondary = OnGoldLight,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = OnGoldContainerDark,
    tertiary = EmeraldLight,
    onTertiary = OnEmeraldLight,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = OnEmeraldContainerDark,
    background = BackgroundDark,
    onBackground = InkLight,
    surface = SurfaceDark,
    onSurface = InkLight,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = InkMutedDark,
    surfaceContainerLowest = BackgroundDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorLight,
    onError = ArenaNavyDeep,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

val GKLeagueShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GKLeagueTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GKLeagueTypography,
        shapes = GKLeagueShapes,
        content = content
    )
}
