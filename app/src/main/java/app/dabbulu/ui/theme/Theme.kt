package app.dabbulu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.dabbulu.R

object Palette {
    val Bg = Color(0xFF0E0F13)
    val SheetTop = Color(0xFF18191F)
    val SheetBottom = Color(0xFF0F1014)
    val Card = Color(0xFF16171D)
    val CardHi = Color(0xFF1F2129)
    val CardLo = Color(0xFF18191F)
    val Field = Color(0xFF1A1C22)
    val Border = Color(0xFF272A33)
    val BorderHi = Color(0xFF34373F)
    val Track = Color(0xFF262932)
    val KeyTop = Color(0xFF2B2E37)
    val KeyBottom = Color(0xFF21242C)
    val KeyEdge = Color(0xFF0A0B0E)
    val Text = Color(0xFFF5F6F8)
    val TextSoft = Color(0xFFC9CCD6)
    val TextDim = Color(0xFFA1A6B3)
    val TextFaint = Color(0xFF6E7381)
    val Accent = Color(0xFF34D399)
    val AccentHi = Color(0xFF7CEBC0)
    val AccentEdge = Color(0xFF126B4D)
    val OnAccent = Color(0xFF07120D)
    val Warn = Color(0xFFFBBF24)
    val Danger = Color(0xFFF87171)
}

val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)

/** Text styles used across the app. */
object Type {
    private fun s(size: Int, weight: FontWeight, spacing: Float = 0f) =
        TextStyle(fontFamily = Jakarta, fontWeight = weight, fontSize = size.sp, letterSpacing = spacing.sp)

    val Hero = s(40, FontWeight.ExtraBold, -1f).copy(fontFeatureSettings = "tnum")
    val H1 = s(28, FontWeight.ExtraBold, -0.5f)
    val H2 = s(20, FontWeight.Bold, -0.2f)
    val Title = s(16, FontWeight.Bold)
    val Body = s(14, FontWeight.Medium)
    val Label = s(13, FontWeight.SemiBold)
    val Small = s(12, FontWeight.Medium)
    val Tiny = s(12, FontWeight.SemiBold)
    val Key = s(23, FontWeight.SemiBold)
    val Button = s(16, FontWeight.ExtraBold, 0.2f)
}

@Composable
fun DabbuluTheme(content: @Composable () -> Unit) {
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Jakarta),
        displayMedium = base.displayMedium.copy(fontFamily = Jakarta),
        displaySmall = base.displaySmall.copy(fontFamily = Jakarta),
        headlineLarge = base.headlineLarge.copy(fontFamily = Jakarta),
        headlineMedium = base.headlineMedium.copy(fontFamily = Jakarta),
        headlineSmall = base.headlineSmall.copy(fontFamily = Jakarta),
        titleLarge = base.titleLarge.copy(fontFamily = Jakarta),
        titleMedium = base.titleMedium.copy(fontFamily = Jakarta),
        titleSmall = base.titleSmall.copy(fontFamily = Jakarta),
        bodyLarge = base.bodyLarge.copy(fontFamily = Jakarta),
        bodyMedium = base.bodyMedium.copy(fontFamily = Jakarta),
        bodySmall = base.bodySmall.copy(fontFamily = Jakarta),
        labelLarge = base.labelLarge.copy(fontFamily = Jakarta),
        labelMedium = base.labelMedium.copy(fontFamily = Jakarta),
        labelSmall = base.labelSmall.copy(fontFamily = Jakarta),
    )
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent,
            onPrimary = Palette.OnAccent,
            secondary = Palette.Accent,
            background = Palette.Bg,
            onBackground = Palette.Text,
            surface = Palette.Card,
            onSurface = Palette.Text,
            surfaceVariant = Palette.CardHi,
            onSurfaceVariant = Palette.TextDim,
            surfaceContainer = Palette.CardHi,
            surfaceContainerHigh = Palette.CardHi,
            surfaceContainerHighest = Palette.CardHi,
            outline = Palette.Border,
            error = Palette.Danger,
        ),
        typography = typography,
        content = content,
    )
}
