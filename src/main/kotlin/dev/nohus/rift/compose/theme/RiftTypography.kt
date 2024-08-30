package dev.nohus.rift.compose.theme

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.nohus.rift.compose.PreviewContainer
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.evesansneue_bold
import dev.nohus.rift.generated.resources.evesansneue_bolditalic
import dev.nohus.rift.generated.resources.evesansneue_italic
import dev.nohus.rift.generated.resources.evesansneue_regular
import dev.nohus.rift.generated.resources.triglavian
import org.jetbrains.compose.resources.Font

@Immutable
data class RiftTypography(
    val captionPrimary: TextStyle,
    val captionSecondary: TextStyle,
    val captionBoldPrimary: TextStyle,
    val bodySpecialHighlighted: TextStyle,
    val bodyHighlighted: TextStyle,
    val bodyPrimary: TextStyle,
    val bodySecondary: TextStyle,
    val bodyLink: TextStyle,
    val bodyTriglavian: TextStyle,
    val titleHighlighted: TextStyle,
    val titlePrimary: TextStyle,
    val titleSecondary: TextStyle,
    val headlineHighlighted: TextStyle,
    val headlinePrimary: TextStyle,
    val headlineSecondary: TextStyle,
)

val LocalRiftTypography = staticCompositionLocalOf {
    RiftTypography(
        captionPrimary = TextStyle.Default,
        captionSecondary = TextStyle.Default,
        captionBoldPrimary = TextStyle.Default,
        bodySpecialHighlighted = TextStyle.Default,
        bodyHighlighted = TextStyle.Default,
        bodyPrimary = TextStyle.Default,
        bodySecondary = TextStyle.Default,
        bodyLink = TextStyle.Default,
        bodyTriglavian = TextStyle.Default,
        titleHighlighted = TextStyle.Default,
        titlePrimary = TextStyle.Default,
        titleSecondary = TextStyle.Default,
        headlineHighlighted = TextStyle.Default,
        headlinePrimary = TextStyle.Default,
        headlineSecondary = TextStyle.Default,
    )
}

@Composable
fun getRiftTypography(colors: RiftColors): RiftTypography {
    val eveSansNeue = FontFamily(
        Font(
            resource = Res.font.evesansneue_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.evesansneue_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic,
        ),
        Font(
            resource = Res.font.evesansneue_bold,
            weight = FontWeight.Bold,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.evesansneue_bolditalic,
            weight = FontWeight.Bold,
            style = FontStyle.Italic,
        ),
    )
    val triglavian = FontFamily(
        Font(
            resource = Res.font.triglavian,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
    )

    val base = TextStyle(
        fontFamily = eveSansNeue,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        letterSpacing = 0.0.sp,
        shadow = Shadow(offset = Offset(1f, 1f), blurRadius = 0f),
    )
    val caption = base.copy(
        fontSize = 11.sp,
    )
    val captionBold = caption.copy(
        fontWeight = FontWeight.Bold,
    )
    val body = base.copy(
        fontSize = 13.sp,
    )
    val bodyBold = body.copy(
        fontWeight = FontWeight.Bold,
    )
    val title = base.copy(
        fontSize = 16.sp,
    )
    val headline = base.copy(
        fontSize = 19.sp,
    )

    val baseTriglavian = base.copy(
        fontFamily = triglavian,
    )
    val bodyTriglavian = baseTriglavian.copy(
        fontSize = 16.sp,
    )

    return RiftTypography(
        captionPrimary = caption.copy(color = colors.textPrimary),
        captionSecondary = caption.copy(color = colors.textSecondary),
        captionBoldPrimary = captionBold.copy(color = colors.textPrimary),
        bodySpecialHighlighted = body.copy(color = colors.textSpecialHighlighted),
        bodyHighlighted = body.copy(color = colors.textHighlighted),
        bodyPrimary = body.copy(color = colors.textPrimary),
        bodySecondary = body.copy(color = colors.textSecondary),
        bodyLink = bodyBold.copy(color = colors.textLink),
        bodyTriglavian = bodyTriglavian,
        titleHighlighted = title.copy(color = colors.textHighlighted),
        titlePrimary = title.copy(color = colors.textPrimary),
        titleSecondary = title.copy(color = colors.textSecondary),
        headlineHighlighted = headline.copy(color = colors.textHighlighted),
        headlinePrimary = headline.copy(color = colors.textPrimary),
        headlineSecondary = headline.copy(color = colors.textSecondary),
    )
}

@Preview
@Composable
private fun RiftTypographyPreview() {
    PreviewContainer {
        Text(
            text = "Caption Primary",
            style = RiftTheme.typography.captionPrimary,
        )
        Text(
            text = "Caption Secondary",
            style = RiftTheme.typography.captionSecondary,
        )
        Text(
            text = "Caption Bold Primary",
            style = RiftTheme.typography.captionBoldPrimary,
        )
        Text(
            text = "Body Special Highlighted – 30%",
            style = RiftTheme.typography.bodySpecialHighlighted,
        )
        Text(
            text = "Body Special Highlighted – 30%",
            style = RiftTheme.typography.bodySpecialHighlighted,
        )
        Text(
            text = "Body Highlighted – Flagship product in the Upwell Consortium's Citadel range of space stations",
            style = RiftTheme.typography.bodyHighlighted,
        )
        Text(
            text = "Body Primary – Structure Damage Limit (per second)",
            style = RiftTheme.typography.bodyPrimary,
        )
        Text(
            text = "Body Secondary – Only one Upwell Palatine Keepstar may be deployed at a time in New Eden",
            style = RiftTheme.typography.bodySecondary,
        )
        Text(
            text = "Headline Highlighted – Pointer Window",
            style = RiftTheme.typography.headlineHighlighted,
        )
        Text(
            text = "Headline Secondary – Log and Messages",
            style = RiftTheme.typography.headlineSecondary,
        )
    }
}
