package dev.anicli.android.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.anicli.android.util.Dimens.CIRCLE_BUTTON_PADDING
import dev.anicli.android.util.Dimens.CIRCLE_BUTTON_SIZE
import dev.anicli.android.util.Dimens.ICON_SIZE
import dev.anicli.android.util.Dimens.ProgressStrokeWidth
import dev.anicli.android.util.Dimens.ProgressBarSize

/** Reference-project circular action button with the progress ring that surrounds it. */
@Composable
fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String = "التالي",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        enabled = enabled,
        modifier = modifier.size(CIRCLE_BUTTON_SIZE),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(CIRCLE_BUTTON_PADDING)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
fun CircularProgressBar(
    modifier: Modifier = Modifier,
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    stroke: Dp = ProgressStrokeWidth,
    size: Dp = ProgressBarSize
) {
    CircularProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.size(size),
        color = color,
        strokeWidth = stroke
    )
}

@Composable
fun SkipTextButton(
    text: String,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
