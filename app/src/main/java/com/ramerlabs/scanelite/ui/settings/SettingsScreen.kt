package com.ramerlabs.scanelite.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramerlabs.scanelite.domain.EdgeStyle
import com.ramerlabs.scanelite.ui.components.BrandHeader
import com.ramerlabs.scanelite.ui.components.RamerLabsCredits
import com.ramerlabs.scanelite.ui.license.LicenseViewModel
import com.ramerlabs.scanelite.ui.session.SessionViewModel
import com.ramerlabs.scanelite.ui.theme.SeBgElevated
import com.ramerlabs.scanelite.ui.theme.SeBgPrimary
import com.ramerlabs.scanelite.ui.theme.SeGold
import com.ramerlabs.scanelite.ui.theme.SeTextPrimary
import com.ramerlabs.scanelite.ui.theme.SeTextSecondary

@Composable
fun SettingsScreen(
    sessionViewModel: SessionViewModel,
    onBack: () -> Unit
) {
    val activity = LocalContext.current as ComponentActivity
    val licenseViewModel: LicenseViewModel = hiltViewModel(activity)
    val state by sessionViewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeBgPrimary)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SeTextPrimary)
        BrandHeader(subtitle = "Crystal-clear scans. Instant share.")

        SettingCard("Auto-capture") {
            Switch(
                checked = state.autoCapture,
                onCheckedChange = sessionViewModel::setAutoCapture,
                colors = SwitchDefaults.colors(checkedTrackColor = SeGold)
            )
        }
        SettingCard("Edge style: ${if (state.edgeStyle == EdgeStyle.Gold) "Gold" else "Neon Blue"}") {
            Switch(
                checked = state.edgeStyle == EdgeStyle.Gold,
                onCheckedChange = {
                    sessionViewModel.setEdgeStyle(if (it) EdgeStyle.Gold else EdgeStyle.NeonBlue)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = SeGold)
            )
        }

        Button(
            onClick = licenseViewModel::replaceLicense,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SeBgElevated, contentColor = SeGold)
        ) {
            Text("Replace license")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("About", fontWeight = FontWeight.SemiBold, color = SeTextPrimary)
        Card(
            colors = CardDefaults.cardColors(containerColor = SeBgElevated),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("ScanElite · v1.0.0", color = SeTextSecondary)
                RamerLabsCredits()
            }
        }

        Text(
            "← Back",
            color = SeGold,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClick = onBack)
        )
    }
}

@Composable
private fun SettingCard(title: String, trailing: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SeBgElevated),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = SeTextPrimary)
            trailing()
        }
    }
}
