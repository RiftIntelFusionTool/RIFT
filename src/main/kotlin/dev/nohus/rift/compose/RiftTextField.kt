package dev.nohus.rift.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.deleteicon
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun RiftTextField(
    text: String,
    icon: DrawableResource? = null,
    placeholder: String? = null,
    isPassword: Boolean = false,
    onTextChanged: (String) -> Unit,
    height: Dp = 32.dp,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = text,
        onValueChange = { onTextChanged(it) },
        textStyle = RiftTheme.typography.bodyPrimary,
        cursorBrush = SolidColor(RiftTheme.colors.borderPrimaryLight),
        singleLine = true,
        visualTransformation = if (isPassword) passwordVisualTransformation else VisualTransformation.None,
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(RiftTheme.colors.windowBackground.copy(alpha = 0.5f))
                    .border(1.dp, RiftTheme.colors.borderGrey)
                    .height(height)
                    .padding(horizontal = 7.dp),
            ) {
                if (icon != null) {
                    val painter = painterResource(icon)
                    Image(
                        painter = painter,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(RiftTheme.colors.textSecondary),
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier
                            .padding(end = 7.dp)
                            .size(16.dp),
                    )
                }
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    innerTextField()
                    if (text.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
                if (onDeleteClick != null && text.isNotEmpty()) {
                    RiftImageButton(
                        resource = Res.drawable.deleteicon,
                        size = 20.dp,
                        onClick = onDeleteClick,
                    )
                }
            }
        },
        modifier = modifier,
    )
}

val passwordVisualTransformation = VisualTransformation {
    TransformedText(
        AnnotatedString("*".repeat(it.text.length)),
        OffsetMapping.Identity,
    )
}
