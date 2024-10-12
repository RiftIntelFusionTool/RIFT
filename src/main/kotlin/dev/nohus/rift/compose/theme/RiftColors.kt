package dev.nohus.rift.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RiftColors(
    val windowBackground: Color,
    val windowBackgroundSecondary: Color,
    val windowBackgroundSecondaryHovered: Color,
    val windowBackgroundActive: Color,

    val textSpecialHighlighted: Color,
    val textHighlighted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textLink: Color,
    val textGreen: Color,
    val textRed: Color,

    val inactiveGray: Color,
    val primary: Color,
    val primaryDark: Color,

    val backgroundPrimary: Color,
    val backgroundPrimaryDark: Color,
    val backgroundPrimaryLight: Color,
    val backgroundSelected: Color,
    val backgroundHovered: Color,
    val backgroundWhite: Color,
    val backgroundError: Color,
    val backgroundErrorDark: Color,

    val borderPrimary: Color,
    val borderPrimaryDark: Color,
    val borderPrimaryLight: Color,
    val borderError: Color,
    val borderGreyLight: Color,
    val borderGrey: Color,
    val borderGreyDropdown: Color,

    val divider: Color,

    val selectionHandle: Color,
    val selectionBackground: Color,

    val dropdownHovered: Color,
    val dropdownSelected: Color,
    val dropdownHighlighted: Color,

    val onlineGreen: Color,
    val awayYellow: Color,
    val extendedAwayOrange: Color,
    val offlineRed: Color,

    val standingTerrible: Color,
    val standingBad: Color,
    val standingGood: Color,
    val standingExcellent: Color,

    val mapBackground: Color,

    val progressBarBackground: Color,
    val progressBarProgress: Color,
)

val LocalRiftColors = staticCompositionLocalOf {
    RiftColors(
        windowBackground = Color.Unspecified,
        windowBackgroundSecondary = Color.Unspecified,
        windowBackgroundSecondaryHovered = Color.Unspecified,
        windowBackgroundActive = Color.Unspecified,

        textSpecialHighlighted = Color.Unspecified,
        textHighlighted = Color.Unspecified,
        textPrimary = Color.Unspecified,
        textSecondary = Color.Unspecified,
        textLink = Color.Unspecified,
        textGreen = Color.Unspecified,
        textRed = Color.Unspecified,

        inactiveGray = Color.Unspecified,
        primary = Color.Unspecified,
        primaryDark = Color.Unspecified,

        backgroundPrimary = Color.Unspecified,
        backgroundPrimaryDark = Color.Unspecified,
        backgroundPrimaryLight = Color.Unspecified,
        backgroundSelected = Color.Unspecified,
        backgroundHovered = Color.Unspecified,
        backgroundWhite = Color.Unspecified,
        backgroundError = Color.Unspecified,
        backgroundErrorDark = Color.Unspecified,

        borderPrimary = Color.Unspecified,
        borderPrimaryDark = Color.Unspecified,
        borderPrimaryLight = Color.Unspecified,
        borderError = Color.Unspecified,
        borderGreyLight = Color.Unspecified,
        borderGrey = Color.Unspecified,
        borderGreyDropdown = Color.Unspecified,

        divider = Color.Unspecified,

        selectionHandle = Color.Unspecified,
        selectionBackground = Color.Unspecified,

        dropdownHovered = Color.Unspecified,
        dropdownSelected = Color.Unspecified,
        dropdownHighlighted = Color.Unspecified,

        onlineGreen = Color.Unspecified,
        awayYellow = Color.Unspecified,
        extendedAwayOrange = Color.Unspecified,
        offlineRed = Color.Unspecified,

        standingTerrible = Color.Unspecified,
        standingBad = Color.Unspecified,
        standingGood = Color.Unspecified,
        standingExcellent = Color.Unspecified,

        mapBackground = Color.Unspecified,

        progressBarBackground = Color.Unspecified,
        progressBarProgress = Color.Unspecified,
    )
}

fun getRiftColors() = RiftColors(
    windowBackground = Color(0xFF070707),
    windowBackgroundSecondary = Color(0xFF141414),
    windowBackgroundSecondaryHovered = Color(0xFF1F272B),
    windowBackgroundActive = Color(0xFF05080A),

    textSpecialHighlighted = Color(0xFFC3E9FF),
    textHighlighted = Color(0xFFE6E6E7),
    textPrimary = Color(0xFFC3C5C6),
    textSecondary = Color(0xFF7E8081),
    textLink = Color(0xFFD98D00),
    textGreen = Color(0xFF029C02),
    textRed = Color(0xFFFB0101),

    inactiveGray = Color(0xFF595555),
    primary = Color(0xFF58A7BF),
    primaryDark = Color(0xFF41707D),

    backgroundPrimary = Color(0xFF172327),
    backgroundPrimaryDark = Color(0xFF0A1215),
    backgroundPrimaryLight = Color(0xFF36525E),
    backgroundSelected = Color(0xFF17262C),
    backgroundHovered = Color(0xFF131C1F),
    backgroundWhite = Color(0xFFEAEAEA),
    backgroundError = Color(0xFF7F2628),
    backgroundErrorDark = Color(0xFF60171A),

    borderPrimary = Color(0xFF335a6a),
    borderPrimaryDark = Color(0xFF213841),
    borderPrimaryLight = Color(0xFF71BED3),
    borderError = Color(0xFFFE5B61),
    borderGreyLight = Color(0xFF303030),
    borderGrey = Color(0xFF1E1E1E),
    borderGreyDropdown = Color(0xFF1E2022),

    divider = Color(0xFF1E2022),

    selectionHandle = Color(0xFF5CADC4),
    selectionBackground = Color(0xFF424344),

    dropdownHovered = Color(0xFF18262D),
    dropdownSelected = Color(0xFF16272E),
    dropdownHighlighted = Color(0xFF273E47),

    onlineGreen = Color(0xFF75D25A),
    awayYellow = Color(0xFFFFD25A),
    extendedAwayOrange = Color(0xFFFF945A),
    offlineRed = Color(0xFFFF494F),

    standingTerrible = Color(0xFFFF494F),
    standingBad = Color(0xFFFF945A),
    standingGood = Color(0xFF316BCA),
    standingExcellent = Color(0xFF0062FF),

    mapBackground = Color(0xFF0A0E15),

    progressBarBackground = Color(0xFF1A1E1F),
    progressBarProgress = Color(0xFF0D557E),
)
