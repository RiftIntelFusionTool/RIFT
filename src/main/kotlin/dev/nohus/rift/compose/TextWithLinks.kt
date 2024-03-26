package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import org.nibor.autolink.LinkExtractor
import org.nibor.autolink.LinkSpan
import org.nibor.autolink.LinkType

private const val URL_TAG = "TAG_URL"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextWithLinks(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    var pointer by remember { mutableStateOf(Cursors.pointer) }
    ClickableText(
        text = text,
        style = style,
        onClick = {
            text.getStringAnnotations(URL_TAG, it, it).firstOrNull()?.let { annotation ->
                annotation.item.toURIOrNull()?.openBrowser()
            }
        },
        onHover = {
            pointer = if (it != null && text.getStringAnnotations(URL_TAG, it, it).isNotEmpty()) {
                Cursors.hand
            } else {
                Cursors.pointer
            }
        },
        modifier = modifier.pointerHoverIcon(PointerIcon(pointer)),
    )
}

@OptIn(ExperimentalTextApi::class)
fun annotateLinks(
    text: String,
    linkStyle: SpanStyle,
): AnnotatedString {
    val linkExtractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL, LinkType.WWW)).build()
    return buildAnnotatedString {
        linkExtractor.extractSpans(text).map { span ->
            val content = text.drop(span.beginIndex).take(span.endIndex - span.beginIndex)
            if (span is LinkSpan) {
                withStyle(linkStyle) {
                    withAnnotation(URL_TAG, content) {
                        append(content)
                    }
                }
            } else {
                append(content)
            }
        }
    }
}
