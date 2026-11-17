package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val WattWiseLightColorScheme =
  lightColorScheme(
    primary = ForestEmerald,
    onPrimary = Color.White,
    primaryContainer = SoftMintWash,
    onPrimaryContainer = ForestEmeraldDark,
    secondary = MidPeakTeal,
    onSecondary = Color.White,
    secondaryContainer = MidPeakTealLight,
    onSecondaryContainer = Color(0xFF083344),
    tertiary = PeakAmber,
    onTertiary = Color.White,
    tertiaryContainer = PeakAmberLight,
    onTertiaryContainer = Color(0xFF78350F),
    background = MintAlabaster,
    onBackground = DeepInk,
    surface = CrispSurface,
    onSurface = DeepInk,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    outline = HairlineSage,
    error = SurgeCoral,
    onError = Color.White,
    errorContainer = SurgeCoralLight,
    onErrorContainer = Color(0xFF7F1D1D),
  )

private val WattWiseDarkColorScheme =
  darkColorScheme(
    primary = LuminousMint,
    onPrimary = Color(0xFF042F22),
    primaryContainer = DeepPineContainer,
    onPrimaryContainer = SoftMintWash,
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = MidPeakTealLight,
    tertiary = PeakAmberDark,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = PeakAmberLight,
    background = ObsidianPine,
    onBackground = SnowMintText,
    surface = DarkCardSurface,
    onSurface = SnowMintText,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = SoftSageText,
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = SurgeCoralLight,
  )

val WattWiseShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun WattWiseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> WattWiseDarkColorScheme
      else -> WattWiseLightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = WattWiseTypography,
    shapes = WattWiseShapes,
    content = content,
  )
}
