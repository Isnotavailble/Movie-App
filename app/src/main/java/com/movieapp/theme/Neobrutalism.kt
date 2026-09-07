package com.movieapp.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Draws a solid, unblurred hard drop-shadow in the Neobrutalism style.
 */
fun Modifier.neoShadow(
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    color: Color = NeoBlack,
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier = this.drawBehind {
    val xPx = offsetX.toPx()
    val yPx = offsetY.toPx()
    if (shape is RoundedCornerShape) {
        val cornerRadius = shape.topStart.toPx(size, this)
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(xPx, yPx),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )
    } else {
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(xPx, yPx),
            size = size
        )
    }
}

/**
 * Standard solid black Neobrutalism border.
 */
fun Modifier.neoBorder(
    width: Dp = 2.5.dp,
    color: Color = NeoBlack,
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier = this.border(width, color, shape)

/**
 * Accessible button with Neobrutalism tactile press translation and >= 48dp touch target.
 */
@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SpideyRed,
    contentColor: Color = WebWhite,
    text: String,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowOffset = if (isPressed) 2.dp else 4.dp

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .neoShadow(offsetX = shadowOffset, offsetY = shadowOffset, shape = shape)
            .offset {
                if (isPressed) IntOffset(2.dp.toPx().roundToInt(), 2.dp.toPx().roundToInt())
                else IntOffset.Zero
            }
            .background(if (enabled) backgroundColor else Color(0xFFE0E0E0), shape)
            .neoBorder(shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontFamily = buttonFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.5.sp
        )
    }
}
