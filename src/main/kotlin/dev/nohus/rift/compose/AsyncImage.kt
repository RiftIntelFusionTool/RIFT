package dev.nohus.rift.compose

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.missing
import dev.nohus.rift.generated.resources.missing_blueprint
import dev.nohus.rift.generated.resources.missing_skin
import dev.nohus.rift.repositories.TypesRepository.Type
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kamel.core.utils.cacheControl
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.client.request.header
import io.ktor.client.utils.CacheControl
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val logger = KotlinLogging.logger {}

/**
 * Shows an image loaded from a URL
 */
@Composable
fun AsyncImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallbackIcon: @Composable () -> Unit = { FallbackIcon() },
    withAnimatedLoading: Boolean = true,
) {
    val painter = asyncPainterResource(url) {
        requestBuilder {
            header("User-Agent", "RIFT (contact: developer@riftforeve.online)")
            cacheControl(CacheControl.MAX_AGE)
        }
    }
    KamelImage(
        resource = painter,
        contentDescription = null,
        contentScale = contentScale,
        onFailure = {
            logger.error { "Failed to load AsyncImage: $url" }
            fallbackIcon()
        },
        animationSpec = if (withAnimatedLoading) tween() else null,
        modifier = modifier,
    )
}

/**
 * Shows an icon of an EVE Online type ID
 */
@Composable
fun AsyncTypeIcon(
    type: Type?,
    modifier: Modifier = Modifier,
) {
    key(type) {
        AsyncTypeIcon(
            typeId = type?.id,
            fallbackIconId = type?.iconId,
            nameHint = type?.name,
            modifier = modifier,
        )
    }
}

/**
 * Shows an icon of an EVE Online type ID
 * nameHint is used to choose a replacement fallback icon
 */
@Composable
fun AsyncTypeIcon(
    typeId: Int?,
    fallbackIconId: Int? = null,
    nameHint: String? = null,
    modifier: Modifier = Modifier,
) {
    if (nameHint != null) {
        val resource = when {
            "Blueprint" in nameHint -> Res.drawable.missing_blueprint
            "SKIN" in nameHint -> Res.drawable.missing_skin
            else -> null
        }
        if (resource != null) {
            FallbackIcon(
                resource = resource,
                modifier = modifier,
            )
            return
        }
    }

    val staticFallbackIcon = @Composable {
        FallbackIcon(
            resource = Res.drawable.missing,
            modifier = modifier,
        )
    }
    val fallbackIcon = @Composable {
        AsyncImage(
            url = "https://images.evetech.net/types/$fallbackIconId/icon",
            modifier = modifier,
            fallbackIcon = staticFallbackIcon,
        )
    }
    val primaryFallbackIcon = if (fallbackIconId != null) fallbackIcon else staticFallbackIcon
    if (typeId != null) {
        AsyncImage(
            url = "https://images.evetech.net/types/$typeId/icon",
            modifier = modifier,
            fallbackIcon = primaryFallbackIcon,
        )
    } else {
        primaryFallbackIcon()
    }
}

/**
 * Shows a portrait of an EVE Online character
 */
@Composable
fun AsyncPlayerPortrait(
    characterId: Int?,
    size: Int,
    modifier: Modifier = Modifier,
    withAnimatedLoading: Boolean = true,
) {
    val effectiveSize = if (LocalDensity.current.density >= 2) size * 2 else size
    AsyncImage(
        url = "https://images.evetech.net/characters/${characterId ?: 0}/portrait?size=$effectiveSize",
        modifier = modifier,
        withAnimatedLoading = withAnimatedLoading,
    )
}

/**
 * Shows a logo of an EVE Online corporation
 */
@Composable
fun AsyncCorporationLogo(
    corporationId: Int?,
    size: Int,
    modifier: Modifier = Modifier,
) {
    val effectiveSize = if (LocalDensity.current.density >= 2) size * 2 else size
    AsyncImage(
        url = "https://images.evetech.net/corporations/${corporationId ?: 0}/logo?size=$effectiveSize",
        modifier = modifier,
    )
}

/**
 * Shows a logo of an EVE Online alliance
 */
@Composable
fun AsyncAllianceLogo(
    allianceId: Int?,
    size: Int,
    modifier: Modifier = Modifier,
) {
    val effectiveSize = if (LocalDensity.current.density >= 2) size * 2 else size
    AsyncImage(
        url = "https://images.evetech.net/alliances/${allianceId ?: 0}/logo?size=$effectiveSize",
        modifier = modifier,
    )
}

@Composable
private fun FallbackIcon(
    resource: DrawableResource = Res.drawable.missing,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        modifier = modifier,
    )
}
