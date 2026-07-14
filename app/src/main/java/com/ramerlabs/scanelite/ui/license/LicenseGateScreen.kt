package com.ramerlabs.scanelite.ui.license

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramerlabs.scanelite.ui.components.BrandHeader
import com.ramerlabs.scanelite.ui.components.RamerLabsCredits
import com.ramerlabs.scanelite.ui.theme.SeBgElevated
import com.ramerlabs.scanelite.ui.theme.SeBgPrimary
import com.ramerlabs.scanelite.ui.theme.SeDanger
import com.ramerlabs.scanelite.ui.theme.SeGold
import com.ramerlabs.scanelite.ui.theme.SeTextPrimary
import com.ramerlabs.scanelite.ui.theme.SeTextSecondary

@Composable
fun LicenseGateScreen(
    viewModel: LicenseViewModel = hiltViewModel(),
    onUnlocked: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.checking) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SeBgPrimary),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = SeGold)
        }
        return
    }
    if (state.unlocked) {
        onUnlocked()
        return
    }

    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeBgPrimary)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        BrandHeader(subtitle = "License required to unlock ScanElite")
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Enter your license key to activate this device.",
            color = SeTextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.keyInput,
            onValueChange = viewModel::onKeyChange,
            label = { Text("License key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SeGold,
                focusedLabelColor = SeGold,
                cursorColor = SeGold,
                focusedTextColor = SeTextPrimary,
                unfocusedTextColor = SeTextPrimary,
                unfocusedContainerColor = SeBgElevated,
                focusedContainerColor = SeBgElevated
            )
        )
        state.error?.let {
            Text(it, color = SeDanger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = viewModel::activate,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SeGold, contentColor = SeBgPrimary)
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    color = SeBgPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(18.dp)
                )
            } else {
                Text("Activate", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(state.buyUrl))
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buy license", color = SeGold)
        }
        Spacer(modifier = Modifier.height(28.dp))
        RamerLabsCredits(modifier = Modifier.fillMaxWidth())
    }
}
