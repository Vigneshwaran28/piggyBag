package com.titanbag.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.titanbag.app.R

// Cosmic Slate
private val CosmicSlateLight = lightColorScheme(
    primary = CosmicSlatePrimaryLight,
    onPrimary = CosmicSlateOnPrimaryLight,
    primaryContainer = CosmicSlatePrimaryContainerLight,
    onPrimaryContainer = CosmicSlateOnPrimaryContainerLight,
    secondary = CosmicSlateSecondaryLight,
    secondaryContainer = CosmicSlateSecondaryContainerLight,
    onSecondaryContainer = CosmicSlateOnSecondaryContainerLight,
    background = CosmicSlateBackgroundLight,
    onBackground = CosmicSlateTextLight,
    surface = CosmicSlateSurfaceLight,
    onSurface = CosmicSlateOnSurfaceLight,
    outline = CosmicSlateOutlineLight,
    outlineVariant = CosmicSlateOutlineVariantLight
)

private val CosmicSlateDark = darkColorScheme(
    primary = CosmicSlatePrimaryDark,
    onPrimary = CosmicSlateOnPrimaryDark,
    primaryContainer = CosmicSlatePrimaryContainerDark,
    onPrimaryContainer = CosmicSlateOnPrimaryContainerDark,
    secondary = CosmicSlateSecondaryDark,
    secondaryContainer = CosmicSlateSecondaryContainerDark,
    onSecondaryContainer = CosmicSlateOnSecondaryContainerDark,
    background = CosmicSlateBackgroundDark,
    onBackground = CosmicSlateTextDark,
    surface = CosmicSlateSurfaceDark,
    onSurface = CosmicSlateOnSurfaceDark,
    outline = CosmicSlateOutlineDark,
    outlineVariant = CosmicSlateOutlineVariantDark
)

// Forest Emerald
private val ForestEmeraldLight = lightColorScheme(
    primary = ForestEmeraldPrimaryLight,
    onPrimary = ForestEmeraldOnPrimaryLight,
    primaryContainer = ForestEmeraldPrimaryContainerLight,
    onPrimaryContainer = ForestEmeraldOnPrimaryContainerLight,
    secondary = ForestEmeraldSecondaryLight,
    secondaryContainer = ForestEmeraldSecondaryContainerLight,
    onSecondaryContainer = ForestEmeraldOnSecondaryContainerLight,
    background = ForestEmeraldBackgroundLight,
    onBackground = ForestEmeraldTextLight,
    surface = ForestEmeraldSurfaceLight,
    onSurface = ForestEmeraldOnSurfaceLight,
    outline = ForestEmeraldOutlineLight,
    outlineVariant = ForestEmeraldOutlineVariantLight
)

private val ForestEmeraldDark = darkColorScheme(
    primary = ForestEmeraldPrimaryDark,
    onPrimary = ForestEmeraldOnPrimaryDark,
    primaryContainer = ForestEmeraldPrimaryContainerDark,
    onPrimaryContainer = ForestEmeraldOnPrimaryContainerDark,
    secondary = ForestEmeraldSecondaryDark,
    secondaryContainer = ForestEmeraldSecondaryContainerDark,
    onSecondaryContainer = ForestEmeraldOnSecondaryContainerDark,
    background = ForestEmeraldBackgroundDark,
    onBackground = ForestEmeraldTextDark,
    surface = ForestEmeraldSurfaceDark,
    onSurface = ForestEmeraldOnSurfaceDark,
    outline = ForestEmeraldOutlineDark,
    outlineVariant = ForestEmeraldOutlineVariantDark
)

// Warm Terracotta
private val WarmTerracottaLight = lightColorScheme(
    primary = WarmTerracottaPrimaryLight,
    onPrimary = WarmTerracottaOnPrimaryLight,
    primaryContainer = WarmTerracottaPrimaryContainerLight,
    onPrimaryContainer = WarmTerracottaOnPrimaryContainerLight,
    secondary = WarmTerracottaSecondaryLight,
    secondaryContainer = WarmTerracottaSecondaryContainerLight,
    onSecondaryContainer = WarmTerracottaOnSecondaryContainerLight,
    background = WarmTerracottaBackgroundLight,
    onBackground = WarmTerracottaTextLight,
    surface = WarmTerracottaSurfaceLight,
    onSurface = WarmTerracottaOnSurfaceLight,
    outline = WarmTerracottaOutlineLight,
    outlineVariant = WarmTerracottaOutlineVariantLight
)

private val WarmTerracottaDark = darkColorScheme(
    primary = WarmTerracottaPrimaryDark,
    onPrimary = WarmTerracottaOnPrimaryDark,
    primaryContainer = WarmTerracottaPrimaryContainerDark,
    onPrimaryContainer = WarmTerracottaOnPrimaryContainerDark,
    secondary = WarmTerracottaSecondaryDark,
    secondaryContainer = WarmTerracottaSecondaryContainerDark,
    onSecondaryContainer = WarmTerracottaOnSecondaryContainerDark,
    background = WarmTerracottaBackgroundDark,
    onBackground = WarmTerracottaTextDark,
    surface = WarmTerracottaSurfaceDark,
    onSurface = WarmTerracottaOnSurfaceDark,
    outline = WarmTerracottaOutlineDark,
    outlineVariant = WarmTerracottaOutlineVariantDark
)

// Deep Indigo
private val DeepIndigoLight = lightColorScheme(
    primary = DeepIndigoPrimaryLight,
    onPrimary = DeepIndigoOnPrimaryLight,
    primaryContainer = DeepIndigoPrimaryContainerLight,
    onPrimaryContainer = DeepIndigoOnPrimaryContainerLight,
    secondary = DeepIndigoSecondaryLight,
    secondaryContainer = DeepIndigoSecondaryContainerLight,
    onSecondaryContainer = DeepIndigoOnSecondaryContainerLight,
    background = DeepIndigoBackgroundLight,
    onBackground = DeepIndigoTextLight,
    surface = DeepIndigoSurfaceLight,
    onSurface = DeepIndigoOnSurfaceLight,
    outline = DeepIndigoOutlineLight,
    outlineVariant = DeepIndigoOutlineVariantLight
)

private val DeepIndigoDark = darkColorScheme(
    primary = DeepIndigoPrimaryDark,
    onPrimary = DeepIndigoOnPrimaryDark,
    primaryContainer = DeepIndigoPrimaryContainerDark,
    onPrimaryContainer = DeepIndigoOnPrimaryContainerDark,
    secondary = DeepIndigoSecondaryDark,
    secondaryContainer = DeepIndigoSecondaryContainerDark,
    onSecondaryContainer = DeepIndigoOnSecondaryContainerDark,
    background = DeepIndigoBackgroundDark,
    onBackground = DeepIndigoTextDark,
    surface = DeepIndigoSurfaceDark,
    onSurface = DeepIndigoOnSurfaceDark,
    outline = DeepIndigoOutlineDark,
    outlineVariant = DeepIndigoOutlineVariantDark
)

// Sunset Gold
private val SunsetGoldLight = lightColorScheme(
    primary = SunsetGoldPrimaryLight,
    onPrimary = SunsetGoldOnPrimaryLight,
    primaryContainer = SunsetGoldPrimaryContainerLight,
    onPrimaryContainer = SunsetGoldOnPrimaryContainerLight,
    secondary = SunsetGoldSecondaryLight,
    secondaryContainer = SunsetGoldSecondaryContainerLight,
    onSecondaryContainer = SunsetGoldOnSecondaryContainerLight,
    background = SunsetGoldBackgroundLight,
    onBackground = SunsetGoldTextLight,
    surface = SunsetGoldSurfaceLight,
    onSurface = SunsetGoldOnSurfaceLight,
    outline = SunsetGoldOutlineLight,
    outlineVariant = SunsetGoldOutlineVariantLight
)

private val SunsetGoldDark = darkColorScheme(
    primary = SunsetGoldPrimaryDark,
    onPrimary = SunsetGoldOnPrimaryDark,
    primaryContainer = SunsetGoldPrimaryContainerDark,
    onPrimaryContainer = SunsetGoldOnPrimaryContainerDark,
    secondary = SunsetGoldSecondaryDark,
    secondaryContainer = SunsetGoldSecondaryContainerDark,
    onSecondaryContainer = SunsetGoldOnSecondaryContainerDark,
    background = SunsetGoldBackgroundDark,
    onBackground = SunsetGoldTextDark,
    surface = SunsetGoldSurfaceDark,
    onSurface = SunsetGoldOnSurfaceDark,
    outline = SunsetGoldOutlineDark,
    outlineVariant = SunsetGoldOutlineVariantDark
)

// Lavender Dream
private val LavenderDreamLight = lightColorScheme(
    primary = LavenderDreamPrimaryLight,
    onPrimary = LavenderDreamOnPrimaryLight,
    primaryContainer = LavenderDreamPrimaryContainerLight,
    onPrimaryContainer = LavenderDreamOnPrimaryContainerLight,
    secondary = LavenderDreamSecondaryLight,
    secondaryContainer = LavenderDreamSecondaryContainerLight,
    onSecondaryContainer = LavenderDreamOnSecondaryContainerLight,
    background = LavenderDreamBackgroundLight,
    onBackground = LavenderDreamTextLight,
    surface = LavenderDreamSurfaceLight,
    onSurface = LavenderDreamOnSurfaceLight,
    outline = LavenderDreamOutlineLight,
    outlineVariant = LavenderDreamOutlineVariantLight
)

private val LavenderDreamDark = darkColorScheme(
    primary = LavenderDreamPrimaryDark,
    onPrimary = LavenderDreamOnPrimaryDark,
    primaryContainer = LavenderDreamPrimaryContainerDark,
    onPrimaryContainer = LavenderDreamOnPrimaryContainerDark,
    secondary = LavenderDreamSecondaryDark,
    secondaryContainer = LavenderDreamSecondaryContainerDark,
    onSecondaryContainer = LavenderDreamOnSecondaryContainerDark,
    background = LavenderDreamBackgroundDark,
    onBackground = LavenderDreamTextDark,
    surface = LavenderDreamSurfaceDark,
    onSurface = LavenderDreamOnSurfaceDark,
    outline = LavenderDreamOutlineDark,
    outlineVariant = LavenderDreamOutlineVariantDark
)

// TitanBag
private val TitanBagLight = lightColorScheme(
    primary = TitanBagPrimaryLight,
    onPrimary = TitanBagOnPrimaryLight,
    primaryContainer = TitanBagPrimaryContainerLight,
    onPrimaryContainer = TitanBagOnPrimaryContainerLight,
    secondary = TitanBagSecondaryLight,
    secondaryContainer = TitanBagSecondaryContainerLight,
    onSecondaryContainer = TitanBagOnSecondaryContainerLight,
    background = TitanBagBackgroundLight,
    onBackground = TitanBagTextLight,
    surface = TitanBagSurfaceLight,
    onSurface = TitanBagOnSurfaceLight,
    outline = TitanBagOutlineLight,
    outlineVariant = TitanBagOutlineVariantLight
)

private val TitanBagDark = darkColorScheme(
    primary = TitanBagPrimaryDark,
    onPrimary = TitanBagOnPrimaryDark,
    primaryContainer = TitanBagPrimaryContainerDark,
    onPrimaryContainer = TitanBagOnPrimaryContainerDark,
    secondary = TitanBagSecondaryDark,
    secondaryContainer = TitanBagSecondaryContainerDark,
    onSecondaryContainer = TitanBagOnSecondaryContainerDark,
    background = TitanBagBackgroundDark,
    onBackground = TitanBagTextDark,
    surface = TitanBagSurfaceDark,
    onSurface = TitanBagOnSurfaceDark,
    outline = TitanBagOutlineDark,
    outlineVariant = TitanBagOutlineVariantDark
)

// Amoled
private val AmoledLight = lightColorScheme(
    primary = AmoledPrimaryLight,
    onPrimary = AmoledOnPrimaryLight,
    primaryContainer = AmoledPrimaryContainerLight,
    onPrimaryContainer = AmoledOnPrimaryContainerLight,
    secondary = AmoledSecondaryLight,
    secondaryContainer = AmoledSecondaryContainerLight,
    onSecondaryContainer = AmoledOnSecondaryContainerLight,
    background = AmoledBackgroundLight,
    onBackground = AmoledTextLight,
    surface = AmoledSurfaceLight,
    onSurface = AmoledOnSurfaceLight,
    outline = AmoledOutlineLight,
    outlineVariant = AmoledOutlineVariantLight
)

private val AmoledDark = darkColorScheme(
    primary = AmoledPrimaryDark,
    onPrimary = AmoledOnPrimaryDark,
    primaryContainer = AmoledPrimaryContainerDark,
    onPrimaryContainer = AmoledOnPrimaryContainerDark,
    secondary = AmoledSecondaryDark,
    secondaryContainer = AmoledSecondaryContainerDark,
    onSecondaryContainer = AmoledOnSecondaryContainerDark,
    background = AmoledBackgroundDark,
    onBackground = AmoledTextDark,
    surface = AmoledSurfaceDark,
    onSurface = AmoledOnSurfaceDark,
    outline = AmoledOutlineDark,
    outlineVariant = AmoledOutlineVariantDark
)

// Plum
private val PlumLight = lightColorScheme(
    primary = PlumPrimaryLight,
    onPrimary = PlumOnPrimaryLight,
    primaryContainer = PlumPrimaryContainerLight,
    onPrimaryContainer = PlumOnPrimaryContainerLight,
    secondary = PlumSecondaryLight,
    secondaryContainer = PlumSecondaryContainerLight,
    onSecondaryContainer = PlumOnSecondaryContainerLight,
    background = PlumBackgroundLight,
    onBackground = PlumTextLight,
    surface = PlumSurfaceLight,
    onSurface = PlumOnSurfaceLight,
    outline = PlumOutlineLight,
    outlineVariant = PlumOutlineVariantLight
)

private val PlumDark = darkColorScheme(
    primary = PlumPrimaryDark,
    onPrimary = PlumOnPrimaryDark,
    primaryContainer = PlumPrimaryContainerDark,
    onPrimaryContainer = PlumOnPrimaryContainerDark,
    secondary = PlumSecondaryDark,
    secondaryContainer = PlumSecondaryContainerDark,
    onSecondaryContainer = PlumOnSecondaryContainerDark,
    background = PlumBackgroundDark,
    onBackground = PlumTextDark,
    surface = PlumSurfaceDark,
    onSurface = PlumOnSurfaceDark,
    outline = PlumOutlineDark,
    outlineVariant = PlumOutlineVariantDark
)

private val PlumMediumLight = lightColorScheme(
    primary = PlumMediumPrimaryLight,
    onPrimary = PlumMediumOnPrimaryLight,
    primaryContainer = PlumMediumPrimaryContainerLight,
    onPrimaryContainer = PlumMediumOnPrimaryContainerLight,
    secondary = PlumMediumSecondaryLight,
    secondaryContainer = PlumMediumSecondaryContainerLight,
    onSecondaryContainer = PlumMediumOnSecondaryContainerLight,
    background = PlumMediumBackgroundLight,
    onBackground = PlumMediumTextLight,
    surface = PlumMediumSurfaceLight,
    onSurface = PlumMediumOnSurfaceLight,
    outline = PlumMediumOutlineLight,
    outlineVariant = PlumMediumOutlineVariantLight
)

private val PlumMediumDark = darkColorScheme(
    primary = PlumMediumPrimaryDark,
    onPrimary = PlumMediumOnPrimaryDark,
    primaryContainer = PlumMediumPrimaryContainerDark,
    onPrimaryContainer = PlumMediumOnPrimaryContainerDark,
    secondary = PlumMediumSecondaryDark,
    secondaryContainer = PlumMediumSecondaryContainerDark,
    onSecondaryContainer = PlumMediumOnSecondaryContainerDark,
    background = PlumMediumBackgroundDark,
    onBackground = PlumMediumTextDark,
    surface = PlumMediumSurfaceDark,
    onSurface = PlumMediumOnSurfaceDark,
    outline = PlumMediumOutlineDark,
    outlineVariant = PlumMediumOutlineVariantDark
)

private val PlumHighLight = lightColorScheme(
    primary = PlumHighPrimaryLight,
    onPrimary = PlumHighOnPrimaryLight,
    primaryContainer = PlumHighPrimaryContainerLight,
    onPrimaryContainer = PlumHighOnPrimaryContainerLight,
    secondary = PlumHighSecondaryLight,
    secondaryContainer = PlumHighSecondaryContainerLight,
    onSecondaryContainer = PlumHighOnSecondaryContainerLight,
    background = PlumHighBackgroundLight,
    onBackground = PlumHighTextLight,
    surface = PlumHighSurfaceLight,
    onSurface = PlumHighOnSurfaceLight,
    outline = PlumHighOutlineLight,
    outlineVariant = PlumHighOutlineVariantLight
)

private val PlumHighDark = darkColorScheme(
    primary = PlumHighPrimaryDark,
    onPrimary = PlumHighOnPrimaryDark,
    primaryContainer = PlumHighPrimaryContainerDark,
    onPrimaryContainer = PlumHighOnPrimaryContainerDark,
    secondary = PlumHighSecondaryDark,
    secondaryContainer = PlumHighSecondaryContainerDark,
    onSecondaryContainer = PlumHighOnSecondaryContainerDark,
    background = PlumHighBackgroundDark,
    onBackground = PlumHighTextDark,
    surface = PlumHighSurfaceDark,
    onSurface = PlumHighOnSurfaceDark,
    outline = PlumHighOutlineDark,
    outlineVariant = PlumHighOutlineVariantDark
)

private val PlumAmoledDark = darkColorScheme(
    primary = PlumAmoledPrimaryDark,
    onPrimary = PlumAmoledOnPrimaryDark,
    primaryContainer = PlumAmoledPrimaryContainerDark,
    onPrimaryContainer = PlumAmoledOnPrimaryContainerDark,
    secondary = PlumAmoledSecondaryDark,
    secondaryContainer = PlumAmoledSecondaryContainerDark,
    onSecondaryContainer = PlumAmoledOnSecondaryContainerDark,
    background = PlumAmoledBackgroundDark,
    onBackground = PlumAmoledTextDark,
    surface = PlumAmoledSurfaceDark,
    onSurface = PlumAmoledOnSurfaceDark,
    outline = PlumAmoledOutlineDark,
    outlineVariant = PlumAmoledOutlineVariantDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPalette: String = "Default",
    customColorHex: String? = null,
    customIconColorHex: String? = null,
    customBgColorHex: String? = null,
    themeModeSetting: String = "system",
    content: @Composable () -> Unit
) {
    val matchingTheme = AppTheme.values().find { it.getFriendlyName() == colorPalette }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var colorScheme = if (colorPalette == "Custom") {
        val primaryColor = try { 
            Color(android.graphics.Color.parseColor(customIconColorHex ?: customColorHex ?: "#6750A4")) 
        } catch (e: Exception) { 
            Color(0xFF6750A4) 
        }
        
        val bgColor = try {
            Color(android.graphics.Color.parseColor(customBgColorHex ?: if (darkTheme) "#121212" else "#FFFFFF"))
        } catch (e: Exception) {
            if (darkTheme) Color(0xFF121212) else Color(0xFFFFFFFF)
        }
        
        generateCustomColorScheme(primaryColor, bgColor, darkTheme)
    } else if (matchingTheme?.isDynamic == true && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (darkTheme) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
    } else if (matchingTheme != null) {
        if (darkTheme) matchingTheme.getDarkColorScheme() else matchingTheme.getLightColorScheme()
    } else {
        if (darkTheme) CosmicSlateDark else CosmicSlateLight
    }

    if (darkTheme) {
        val backgroundOverride = when (themeModeSetting) {
            "soft_dark" -> Color(0xFF1E1E1E)
            "pure_black" -> Color(0xFF000000)
            else -> null
        }
        if (backgroundOverride != null) {
            colorScheme = colorScheme.copy(
                background = backgroundOverride,
                surface = backgroundOverride,
                surfaceVariant = backgroundOverride.lighten(0.05f),
                onBackground = Color.White,
                onSurface = Color.White
            )
        }
    }

    val currentDensity = LocalDensity.current
    val fontScaleFreeDensity = Density(currentDensity.density, 1f)

    CompositionLocalProvider(
        LocalDensity provides fontScaleFreeDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = {
                CompositionLocalProvider(
                    LocalSpacing provides Spacing()
                ) {
                    content()
                }
            }
        )
    }
}

fun generateCustomColorScheme(primary: Color, background: Color, isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = if (primary.red + primary.green + primary.blue > 1.5f) Color.Black else Color.White,
            primaryContainer = primary.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            secondary = primary.copy(alpha = 0.8f),
            onSecondary = Color.White,
            background = background,
            surface = background,
            onBackground = Color.White,
            onSurface = Color.White,
            surfaceVariant = background.lighten(0.05f),
            onSurfaceVariant = Color.LightGray
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = if (primary.red + primary.green + primary.blue > 1.5f) Color.Black else Color.White,
            primaryContainer = primary.copy(alpha = 0.2f),
            onPrimaryContainer = Color.Black,
            secondary = primary.copy(alpha = 0.8f),
            onSecondary = Color.Black,
            background = background,
            surface = background,
            onBackground = Color.Black,
            onSurface = Color.Black,
            surfaceVariant = background.darken(0.03f),
            onSurfaceVariant = Color.DarkGray
        )
    }
}

fun generateColorSchemeFromPrimary(primary: Color, isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = primary.darken(0.8f),
            primaryContainer = primary.darken(0.5f),
            onPrimaryContainer = primary.lighten(0.3f),
            secondary = primary.desaturate(0.5f).lighten(0.2f),
            onSecondary = primary.darken(0.8f),
            background = Color(0xFF121212),
            surface = Color(0xFF121212),
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.lighten(0.7f),
            onPrimaryContainer = primary.darken(0.4f),
            secondary = primary.desaturate(0.3f).darken(0.2f),
            onSecondary = Color.White,
            background = Color.White,
            surface = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    }
}

// Helper for color generation
private fun Color.desaturate(factor: Float): Color {
    // Very simple desaturation
    val luminance = (red + green + blue) / 3f
    return Color(
        red = red * (1 - factor) + luminance * factor,
        green = green * (1 - factor) + luminance * factor,
        blue = blue * (1 - factor) + luminance * factor,
        alpha = alpha
    )
}

enum class AppTheme(
    val titleRes: Int,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
    val tertiaryLight: Color,
    val tertiaryDark: Color,
    val backgroundLight: Color,
    val backgroundDark: Color,
    val isDynamic: Boolean = false
) {
    MaterialYou(
        titleRes = R.string.theme_material_you,
        primaryLight = Color(0xFF6750A4),
        primaryDark = Color(0xFFD0BCFF),
        secondaryLight = Color(0xFF625B71),
        secondaryDark = Color(0xFFCCC2DC),
        tertiaryLight = Color(0xFF7D5260),
        tertiaryDark = Color(0xFFEFB8C8),
        backgroundLight = Color(0xFFFFFBFF),
        backgroundDark = Color(0xFF1C1B1F),
        isDynamic = true,
    ),
    Catppuccin(
        titleRes = R.string.theme_catppuccin,
        primaryLight = Color(0xFF4C6B9A),
        primaryDark = Color(0xFF9BA8CF),
        secondaryLight = Color(0xFFB76B8F),
        secondaryDark = Color(0xFFD4A5B8),
        tertiaryLight = Color(0xFFB8763E),
        tertiaryDark = Color(0xFF8AB8A8),
        backgroundLight = Color(0xFFEFF1F5),
        backgroundDark = Color(0xFF1E1E2E),
    ),
    Nord(
        titleRes = R.string.theme_nord,
        primaryLight = Color(0xFF5E81AC),
        primaryDark = Color(0xFF88C0D0),
        secondaryLight = Color(0xFF4C566A),
        secondaryDark = Color(0xFFD8DEE9),
        tertiaryLight = Color(0xFFB48EAD),
        tertiaryDark = Color(0xFFD8A9C4),
        backgroundLight = Color(0xFFECEFF4),
        backgroundDark = Color(0xFF2E3440),
    ),
    Dracula(
        titleRes = R.string.theme_dracula,
        primaryLight = Color(0xFF6272A4),
        primaryDark = Color(0xFFBD93F9),
        secondaryLight = Color(0xFF44475A),
        secondaryDark = Color(0xFFFF79C6),
        tertiaryLight = Color(0xFF50FA7B),
        tertiaryDark = Color(0xFF8BE9FD),
        backgroundLight = Color(0xFFF8F8F2),
        backgroundDark = Color(0xFF282A36),
    ),
    TokyoNight(
        titleRes = R.string.theme_tokyo_night,
        primaryLight = Color(0xFF3D5A80),
        primaryDark = Color(0xFF7D9BC1),
        secondaryLight = Color(0xFF6B5B95),
        secondaryDark = Color(0xFFA89DC9),
        tertiaryLight = Color(0xFF4A6B5C),
        tertiaryDark = Color(0xFF8AB4A3),
        backgroundLight = Color(0xFFF0F1F5),
        backgroundDark = Color(0xFF1A1B26),
    ),
    RosePine(
        titleRes = R.string.theme_rose_pine,
        primaryLight = Color(0xFF907AA9),
        primaryDark = Color(0xFFC4A7E7),
        secondaryLight = Color(0xFFB4637A),
        secondaryDark = Color(0xFFEBBCBA),
        tertiaryLight = Color(0xFF7A9A8A),
        tertiaryDark = Color(0xFF9CCFD8),
        backgroundLight = Color(0xFFFAF4ED),
        backgroundDark = Color(0xFF232136),
    ),
    GreenApple(
        titleRes = R.string.theme_green_apple,
        primaryLight = Color(0xFF2E7D32),
        primaryDark = Color(0xFF81C784),
        secondaryLight = Color(0xFF4A6349),
        secondaryDark = Color(0xFFB0CFB1),
        tertiaryLight = Color(0xFF3D7B5F),
        tertiaryDark = Color(0xFF8FD5B7),
        backgroundLight = Color(0xFFF6FFF6),
        backgroundDark = Color(0xFF0F1A0F),
    ),
    Midnight(
        titleRes = R.string.theme_midnight,
        primaryLight = Color(0xFF0D47A1),
        primaryDark = Color(0xFF90CAF9),
        secondaryLight = Color(0xFF455A64),
        secondaryDark = Color(0xFFB0BEC5),
        tertiaryLight = Color(0xFF1565C0),
        tertiaryDark = Color(0xFF64B5F6),
        backgroundLight = Color(0xFFF5F9FF),
        backgroundDark = Color(0xFF0D1117),
    ),
    Lavender(
        titleRes = R.string.theme_lavender,
        primaryLight = Color(0xFF7C5AB8),
        primaryDark = Color(0xFFCFBCFF),
        secondaryLight = Color(0xFF635B70),
        secondaryDark = Color(0xFFCBC3DA),
        tertiaryLight = Color(0xFF7E525A),
        tertiaryDark = Color(0xFFF2B8C1),
        backgroundLight = Color(0xFFFCF8FF),
        backgroundDark = Color(0xFF16121A),
    ),
    Sunset(
        titleRes = R.string.theme_sunset,
        primaryLight = Color(0xFFE65100),
        primaryDark = Color(0xFFFF9E80),
        secondaryLight = Color(0xFFEF6C00),
        secondaryDark = Color(0xFFFFCC80),
        tertiaryLight = Color(0xFFF4511E),
        tertiaryDark = Color(0xFFFF8A65),
        backgroundLight = Color(0xFFFFF5F0),
        backgroundDark = Color(0xFF1A120D),
    );

    /**
     * Get the light color scheme for this theme
     */
    fun getLightColorScheme(): ColorScheme {
        val surfaceTint = primaryLight.copy(alpha = 0.05f).compositeOver(backgroundLight)
        return lightColorScheme(
            primary = primaryLight,
            onPrimary = Color.White,
            primaryContainer = primaryLight.copy(alpha = 0.15f).compositeOver(Color.White),
            onPrimaryContainer = primaryLight.darken(0.3f),
            secondary = secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = secondaryLight.copy(alpha = 0.15f).compositeOver(Color.White),
            onSecondaryContainer = secondaryLight.darken(0.3f),
            tertiary = tertiaryLight,
            onTertiary = Color.White,
            tertiaryContainer = tertiaryLight.copy(alpha = 0.15f).compositeOver(Color.White),
            onTertiaryContainer = tertiaryLight.darken(0.3f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF93000A),
            background = backgroundLight,
            onBackground = Color(0xFF1C1B1F),
            surface = backgroundLight,
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = primaryLight.copy(alpha = 0.08f).compositeOver(Color(0xFFF0F0F0)),
            onSurfaceVariant = Color(0xFF49454F),
            outline = secondaryLight.copy(alpha = 0.5f).compositeOver(Color(0xFF79747E)),
            outlineVariant = primaryLight.copy(alpha = 0.12f).compositeOver(Color(0xFFCAC4D0)),
            inverseSurface = backgroundDark,
            inverseOnSurface = Color(0xFFF4EFF4),
            inversePrimary = primaryDark,
            surfaceContainerLowest = backgroundLight,
            surfaceContainerLow = surfaceTint,
            surfaceContainer = primaryLight.copy(alpha = 0.06f).compositeOver(backgroundLight),
            surfaceContainerHigh = primaryLight.copy(alpha = 0.08f).compositeOver(backgroundLight),
            surfaceContainerHighest = primaryLight.copy(alpha = 0.11f).compositeOver(backgroundLight),
        )
    }

    /**
     * Get the dark color scheme for this theme
     */
    fun getDarkColorScheme(): ColorScheme {
        val surfaceTint = primaryDark.copy(alpha = 0.05f).compositeOver(backgroundDark)
        return darkColorScheme(
            primary = primaryDark,
            onPrimary = primaryLight.darken(0.5f),
            primaryContainer = primaryLight.darken(0.3f),
            onPrimaryContainer = primaryDark.lighten(0.1f),
            secondary = secondaryDark,
            onSecondary = secondaryLight.darken(0.5f),
            secondaryContainer = secondaryLight.darken(0.3f),
            onSecondaryContainer = secondaryDark.lighten(0.1f),
            tertiary = tertiaryDark,
            onTertiary = tertiaryLight.darken(0.5f),
            tertiaryContainer = tertiaryLight.darken(0.3f),
            onTertiaryContainer = tertiaryDark.lighten(0.1f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = backgroundDark,
            onBackground = Color(0xFFE6E1E5),
            surface = backgroundDark,
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = primaryDark.copy(alpha = 0.12f).compositeOver(Color(0xFF2A2A2A)),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = secondaryDark.copy(alpha = 0.4f).compositeOver(Color(0xFF938F99)),
            outlineVariant = primaryDark.copy(alpha = 0.15f).compositeOver(Color(0xFF49454F)),
            inverseSurface = backgroundLight,
            inverseOnSurface = Color(0xFF313033),
            inversePrimary = primaryLight,
            surfaceContainerLowest = backgroundDark.darken(0.2f),
            surfaceContainerLow = surfaceTint,
            surfaceContainer = primaryDark.copy(alpha = 0.05f).compositeOver(backgroundDark),
            surfaceContainerHigh = primaryDark.copy(alpha = 0.08f).compositeOver(backgroundDark),
            surfaceContainerHighest = primaryDark.copy(alpha = 0.11f).compositeOver(backgroundDark),
        )
    }

    /**
     * Get the AMOLED (pure black) color scheme for this theme
     */
    fun getAmoledColorScheme(): ColorScheme = getDarkColorScheme().copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = primaryDark.copy(alpha = 0.08f).compositeOver(Color(0xFF1A1A1A)),
        surfaceContainer = Color(0xFF0A0A0A),
        surfaceContainerLow = Color(0xFF050505),
        surfaceContainerLowest = Color.Black,
        surfaceContainerHigh = primaryDark.copy(alpha = 0.05f).compositeOver(Color(0xFF151515)),
        surfaceContainerHighest = primaryDark.copy(alpha = 0.08f).compositeOver(Color(0xFF1F1F1F)),
        surfaceDim = Color.Black,
        surfaceBright = primaryDark.copy(alpha = 0.06f).compositeOver(Color(0xFF2A2A2A)),
    )
}

fun AppTheme.getFriendlyName(): String {
    return when (this) {
        AppTheme.MaterialYou -> "Material You"
        AppTheme.TokyoNight -> "Tokyo Night"
        AppTheme.RosePine -> "Rose Pine"
        AppTheme.GreenApple -> "Green Apple"
        else -> name
    }
}

// Extension functions for color manipulation
private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1 - factor)).coerceIn(0f, 1f),
        green = (green * (1 - factor)).coerceIn(0f, 1f),
        blue = (blue * (1 - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.lighten(factor: Float): Color {
    return Color(
        red = (red + (1 - red) * factor).coerceIn(0f, 1f),
        green = (green + (1 - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1 - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.compositeOver(background: Color): Color {
    val bgAlpha = background.alpha
    val fgAlpha = alpha
    val a = fgAlpha + bgAlpha * (1f - fgAlpha)
    return if (a == 0f) {
        Color.Transparent
    } else {
        Color(
            red = (red * fgAlpha + background.red * bgAlpha * (1f - fgAlpha)) / a,
            green = (green * fgAlpha + background.green * bgAlpha * (1f - fgAlpha)) / a,
            blue = (blue * fgAlpha + background.blue * bgAlpha * (1f - fgAlpha)) / a,
            alpha = a
        )
    }
}
