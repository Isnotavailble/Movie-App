package com.movieapp.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Spider-Man Neobrutalism Solid Base Accents (Zero Gradients)
val SpideyRed = Color(0xFFE23636)
val SpideyRedDark = Color(0xFFE52E42)
val SpideyBlue = Color(0xFF0055FF)
val SpideyBlueDark = Color(0xFF2563EB)
val WebGold = Color(0xFFFFC700)
val WebGoldDark = Color(0xFFFBBF24)
val WebBlack = Color(0xFF000000)
val WebWhite = Color(0xFFFFFFFF)

// Classic Neobrutalism Base Accents (Mapped to Spidey Theme)
val NeoYellow = WebGold
val NeoCyan = SpideyBlue
val NeoPink = SpideyRed
val NeoGreen = Color(0xFF00E599)
val NeoBlack = WebBlack
val NeoWhite = WebWhite

// Static Fallbacks for existing imports
val NeoBackground = Color(0xFFF8F9FD)
val NeoCardBg = Color(0xFFFFFFFF)
val NeoMuted = Color(0xFF64748B)
val NeoBorder = Color(0xFF000000)
val NeoError = Color(0xFFD32F2F)
val NeoErrorBackground = Color(0xFFFFEBEE)

@Immutable
data class NeoColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val shadow: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val error: Color,
    val errorBackground: Color,
    val onPrimary: Color = WebWhite,
    val onSecondary: Color = WebWhite,
    val onError: Color = WebWhite
)

// 1. Classic Spider-Man Suit Light Mode (Solid Flat Colors, Zero Gradients)
val LightNeoColors = NeoColors(
    background = Color(0xFFF8F9FD),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEEF2F9),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    border = Color(0xFF000000),
    shadow = Color(0xFF000000),
    primary = SpideyRed,
    secondary = SpideyBlue,
    tertiary = WebGold,
    error = Color(0xFFC62828),
    errorBackground = Color(0xFFFFEBEE),
    onPrimary = WebWhite,
    onSecondary = WebWhite
)

// 2. Symbiote / Stealth Suit Dark Mode (Solid Flat Colors, Zero Gradients)
val DarkNeoColors = NeoColors(
    background = Color(0xFF0A0E17),
    surface = Color(0xFF121826),
    surfaceMuted = Color(0xFF1A2234),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    border = Color(0xFF000000),
    shadow = Color(0xFF000000),
    primary = SpideyRedDark,
    secondary = SpideyBlueDark,
    tertiary = WebGoldDark,
    error = Color(0xFFFF4D4D),
    errorBackground = Color(0xFF2D1418),
    onPrimary = WebWhite,
    onSecondary = WebWhite
)
