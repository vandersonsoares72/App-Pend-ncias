package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class KanbanExtendedColors(
    val pendingAccent: Color,
    val pendingContainer: Color,
    val onPendingContainer: Color,
    val startedAccent: Color,
    val startedContainer: Color,
    val onStartedContainer: Color,
    val doneAccent: Color,
    val doneContainer: Color,
    val onDoneContainer: Color,
    val highPriority: Color,
    val highPriorityContainer: Color,
    val cardBorder: Color,
    val columnSurface: Color
)

private val LightExtendedColors = KanbanExtendedColors(
    pendingAccent = PendingAmberAccent,
    pendingContainer = PendingAmberContainer,
    onPendingContainer = OnPendingAmberContainer,
    startedAccent = StartedAzureAccent,
    startedContainer = StartedAzureContainer,
    onStartedContainer = OnStartedAzureContainer,
    doneAccent = DoneEmeraldAccent,
    doneContainer = DoneEmeraldContainer,
    onDoneContainer = OnDoneEmeraldContainer,
    highPriority = PriorityHigh,
    highPriorityContainer = PriorityHighContainer,
    cardBorder = StudioStoneBorder,
    columnSurface = StudioStone
)

private val DarkExtendedColors = KanbanExtendedColors(
    pendingAccent = PendingAmberAccent,
    pendingContainer = DarkPendingContainer,
    onPendingContainer = DarkPendingOnContainer,
    startedAccent = StartedAzureAccent,
    startedContainer = DarkStartedContainer,
    onStartedContainer = DarkStartedOnContainer,
    doneAccent = DoneEmeraldAccent,
    doneContainer = DarkDoneContainer,
    onDoneContainer = DarkDoneOnContainer,
    highPriority = DarkHighOnContainer,
    highPriorityContainer = DarkHighContainer,
    cardBorder = DarkBorder,
    columnSurface = DarkSurface
)

val LocalKanbanColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = BrandNavy,
    onPrimary = Color.White,
    primaryContainer = BrandNavyContainer,
    onPrimaryContainer = OnBrandNavyContainer,
    secondary = StartedAzure,
    onSecondary = Color.White,
    secondaryContainer = StartedAzureContainer,
    onSecondaryContainer = OnStartedAzureContainer,
    tertiary = DoneEmerald,
    onTertiary = Color.White,
    tertiaryContainer = DoneEmeraldContainer,
    onTertiaryContainer = OnDoneEmeraldContainer,
    background = StudioAlabaster,
    onBackground = StudioInk,
    surface = StudioPaper,
    onSurface = StudioInk,
    surfaceVariant = StudioStone,
    onSurfaceVariant = StudioSlate,
    outline = StudioStoneBorder,
    outlineVariant = Color(0xFFE7E2D8),
    error = PriorityHigh,
    onError = Color.White,
    errorContainer = PriorityHighContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = OnDarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = OnDarkPrimaryContainer,
    secondary = StartedAzureAccent,
    onSecondary = OnDarkPrimary,
    secondaryContainer = DarkStartedContainer,
    onSecondaryContainer = DarkStartedOnContainer,
    tertiary = DoneEmeraldAccent,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = DarkDoneContainer,
    onTertiaryContainer = DarkDoneOnContainer,
    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkSurfaceElevated,
    onSurface = DarkInk,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkSlate,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1F2C40),
    error = DarkHighOnContainer,
    onError = Color(0xFF450A0A),
    errorContainer = DarkHighContainer,
    onErrorContainer = DarkHighOnContainer
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FluxoKanbanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalKanbanColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
