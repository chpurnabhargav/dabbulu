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

/** A calm, near-black palette with one mint accent. Depth comes from layered surfaces, not decoration. */
object Palette {
    val Bg = Color(0xFF0A0B0E)
    val SheetTop = Color(0xFF16171C)
    val SheetBottom = Color(0xFF101115)
    val Card = Color(0xFF131418)
    val CardHi = Color(0xFF1A1B21)
    val Field = Color(0xFF1B1C22)
    val Border = Color(0xFF22242B)
    val BorderHi = Color(0xFF2F313A)
    val Track = Color(0xFF22242B)
    val KeyTop = Color(0xFF1E1F25)
    val KeyBottom = Color(0xFF1A1B20)
    val KeyEdge = Color(0xFF060709)
    val Text = Color(0xFFF4F5F7)
    val TextSoft = Color(0xFFC9CCD4)
    val TextDim = Color(0xFF9399A6)
    val TextFaint = Color(0xFF656B78)
    val Accent = Color(0xFF3DDC97)
    val AccentEdge = Color(0xFF167A52)
    val OnAccent = Color(0xFF04140C)
    val Good = Color(0xFF3DDC97)
    val Warn = Color(0xFFF5B94A)
    val Danger = Color(0xFFF2726F)
}

val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)

object Type {
    private fun s(size: Int, weight: FontWeight, spacing: Float = 0f) =
        TextStyle(fontFamily = Jakarta, fontWeight = weight, fontSize = size.sp, letterSpacing = spacing.sp)

    val Hero = s(40, FontWeight.Bold, -1.2f).copy(fontFeatureSettings = "tnum")
    val Amount = s(28, FontWeight.Bold, -0.6f).copy(fontFeatureSettings = "tnum")
    val H1 = s(26, FontWeight.Bold, -0.5f)
    val H2 = s(18, FontWeight.Bold, -0.2f)
    val Title = s(15, FontWeight.SemiBold)
    val Body = s(14, FontWeight.Medium)
    val Label = s(13, FontWeight.SemiBold)
    val Small = s(12, FontWeight.Medium)
    val Tiny = s(11, FontWeight.SemiBold, 0.2f)
    val Key = s(24, FontWeight.Medium)
    val Button = s(16, FontWeight.Bold, 0.1f)
    val Num = s(15, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum")
}

@Composable
fun DabbuluTheme(content: @Composable () -> Unit) {
    val base = Typography()
    fun TextStyle.j() = copy(fontFamily = Jakarta)
    val typography = Typography(
        displayLarge = base.displayLarge.j(), displayMedium = base.displayMedium.j(), displaySmall = base.displaySmall.j(),
        headlineLarge = base.headlineLarge.j(), headlineMedium = base.headlineMedium.j(), headlineSmall = base.headlineSmall.j(),
        titleLarge = base.titleLarge.j(), titleMedium = base.titleMedium.j(), titleSmall = base.titleSmall.j(),
        bodyLarge = base.bodyLarge.j(), bodyMedium = base.bodyMedium.j(), bodySmall = base.bodySmall.j(),
        labelLarge = base.labelLarge.j(), labelMedium = base.labelMedium.j(), labelSmall = base.labelSmall.j(),
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
