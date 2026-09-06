package com.movieapp.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.movieapp.R

/**
 * Neobrutalism Custom Typography System using authentic local fonts:
 * - Hero Title / Big Display: blacktofu_font
 * - Badges / Tags / Retro Accents: typewriter_font
 * - Buttons / Subheadings: cartoon_font (for playful pop)
 * - Body Paragraphs: yoeshin_font Regular
 */
val BlackTofuFontFamily = FontFamily(Font(R.font.blacktofu_font, FontWeight.Black))
val TypewriterFontFamily = FontFamily(Font(R.font.typewriter_font, FontWeight.Normal))
val CartoonFontFamily = FontFamily(Font(R.font.cartoon_font, FontWeight.Bold))
val YoeshinFontFamily = FontFamily(Font(R.font.yoeshin_font, FontWeight.Normal))

// Full SIL Padauk font family with complete OpenType (mym2/mymr) complex shaping for Myanmar Unicode
val PadaukFontFamily = FontFamily(
    Font(R.font.padauk_regular, FontWeight.Normal),
    Font(R.font.padauk_bold, FontWeight.Bold)
)

/**
 * Header font selector based on the active language:
 * - Myanmar (MY): Uses PadaukFontFamily for authentic, fully shaped OpenType Unicode rendering.
 * - English (EN) & others: Uses CartoonFontFamily for comic book display.
 */
@Composable
fun headerFontFamily(): FontFamily {
    return if (com.movieapp.util.LocalizationManager.currentLanguage == com.movieapp.util.AppLanguage.MY) {
        PadaukFontFamily
    } else {
        CartoonFontFamily
    }
}

/**
 * Button and interactive label font selector based on the active language:
 * - Myanmar (MY): Uses PadaukFontFamily (Bold) for clean, unclipped Burmese interactive labels.
 * - English (EN) & others: Uses CartoonFontFamily for comic book aesthetic.
 */
@Composable
fun buttonFontFamily(): FontFamily {
    return if (com.movieapp.util.LocalizationManager.currentLanguage == com.movieapp.util.AppLanguage.MY) {
        PadaukFontFamily
    } else {
        CartoonFontFamily
    }
}

/**
 * Body text font selector based on the active language:
 * - Myanmar (MY): Uses PadaukFontFamily for smooth paragraph legibility without broken diacritics.
 * - English (EN) & others: Uses YoeshinFontFamily or Default font.
 */
@Composable
fun bodyFontFamily(): FontFamily {
    return if (com.movieapp.util.LocalizationManager.currentLanguage == com.movieapp.util.AppLanguage.MY) {
        PadaukFontFamily
    } else {
        YoeshinFontFamily
    }
}

/**
 * Badge and tag font selector:
 * - Myanmar (MY): Uses PadaukFontFamily.
 * - English (EN): Uses TypewriterFontFamily for monospaced retro look.
 */
@Composable
fun badgeFontFamily(): FontFamily {
    return if (com.movieapp.util.LocalizationManager.currentLanguage == com.movieapp.util.AppLanguage.MY) {
        PadaukFontFamily
    } else {
        TypewriterFontFamily
    }
}

val Typography = Typography(
    // Big Hero Display (App Title, Top Banner) - defaults to Cartoon for EN, BlackTofu for MY via headerFontFamily()
    displayLarge = TextStyle(
        fontFamily = CartoonFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = CartoonFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    // Subheadings & Interactive Buttons (Playful Cartoon Pop)
    titleLarge = TextStyle(
        fontFamily = CartoonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CartoonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = CartoonFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    // Body Paragraphs (Clean & Readable Yoeshin Regular)
    bodyLarge = TextStyle(
        fontFamily = YoeshinFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = YoeshinFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = YoeshinFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    // Badges, Tags, & Retro Metadata Accents (Typewriter Monospaced)
    labelLarge = TextStyle(
        fontFamily = TypewriterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = TypewriterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = TypewriterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )
)
