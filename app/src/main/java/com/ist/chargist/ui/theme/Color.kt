package com.ist.chargist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ist.chargist.R


val blueBG = Color(0xFFF4F7FD)
//val card = Color(0xFFFFFFFF)

//Add Image in Add Collection Colors
val imageAdd = Color(0xFFE1E2E9)
val imageAddFont = Color(0xFF074473)
val imageAddCloudIcon = Color(0xFF074473)
val imageAddCenterIcon = Color(0xFFB2C5D4)

//Dialog Colors
val dialogBackground = Color(0xFFFFFFFF)
val dialogBackgroundDark = Color(0xd9858C94)

//AssetHistoryCard
val AssetHistoryCard = Color(0xFFFFFFFF)
val AssetHistoryCardDark = Color(0xFF2E2E2E)

//Other Colors
val OrangePressed = Color(0xFFFF7F50)
val darkBackground = Color(0xFF000000)

//BulkChip Colors
val ReceivingFound = Color(0x340FA00C)
val FoundText = Color(0xFF0D9801)
val ReceivingNotFound = Color(0x33E5313C)
val NotFoundText = Color.Red

// Button Colors
val ISTBlue = Color(0xFF00A0E4)

//Small Action Button Label
val OrangeBackDarkLabel = Color(0xE9AC3E16)
val OrangeBackLightLabel = Color(0xCCFF946E)

//AssetChip Color
val AssetColorDark = Color(0xFF262626)

//AssetBorder Color
val AssetBorderColor = Color(0xFFB3B3B3)

//MissingAssetRow Color
val MissingAssetRowColor = Color(0x19121212)
val MissingAssetRowColorDark = Color(0x26FFFFFF)

//AssetHistoryChip Color
val PendingAssetColor = Color(0xFFF04438)
val ReceivedAssetColor = Color(0xFF039855)

//LoginContainer Color
val LoginContainerColor = Color(0xFFE4E4E4)


val BackgroundColor: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) blueBG else darkBackground

val MissingAssetCardColor: Color
    @Composable
    get() = DialogBackground

val TextColor: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) Color.Black else Color.White

val LineDividerColor: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) MissingAssetRowColor else MissingAssetRowColorDark

val DialogBackground: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) dialogBackground else dialogBackgroundDark

val ImageDialog: Int
    @Composable
    get() = if (!isSystemInDarkTheme()) R.drawable.ic_dialog_light else R.drawable.ic_dialog_dark

val HorizontalDividerColor: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) AssetHistoryCardDark else AssetHistoryCard

val AssetHistoryItemColor: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) AssetHistoryCard else AssetHistoryCardDark

val SmallButtonLabel: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) OrangeBackLightLabel else OrangeBackDarkLabel

val AssetChipColor: Color
    @Composable
    get() = if (!isSystemInDarkTheme()) Color.White else AssetColorDark
