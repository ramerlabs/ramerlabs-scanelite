package com.ramerlabs.scanelite.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramerlabs.scanelite.R
import com.ramerlabs.scanelite.ui.theme.SeGold
import com.ramerlabs.scanelite.ui.theme.SeTextSecondary

@Composable
fun ScanEliteLogoMark(size: Dp = 48.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_scanelite_icon),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier.size(size)
    )
}

@Composable
fun RamerLabsCredits(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.credits_url)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(stringResource(R.string.credits_label), color = SeTextSecondary, fontSize = 12.sp)
        Text(
            text = stringResource(R.string.credits_url_label),
            color = SeGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { uriHandler.openUri(url) }
        )
    }
}

@Composable
fun BrandHeader(subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScanEliteLogoMark(size = 44.dp)
        Column {
            Text(
                text = stringResource(R.string.app_name),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(subtitle, color = SeTextSecondary, fontSize = 13.sp)
        }
    }
}
