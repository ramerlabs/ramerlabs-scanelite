package com.ramerlabs.scanelite.ui.editor

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramerlabs.scanelite.domain.ScanFilter
import com.ramerlabs.scanelite.domain.ScanMode
import com.ramerlabs.scanelite.ui.session.SessionViewModel
import com.ramerlabs.scanelite.ui.theme.SeBgElevated
import com.ramerlabs.scanelite.ui.theme.SeBgPrimary
import com.ramerlabs.scanelite.ui.theme.SeGold
import com.ramerlabs.scanelite.ui.theme.SeTextPrimary
import com.ramerlabs.scanelite.ui.theme.SeTextSecondary

@Composable
fun PageEditorScreen(
    sessionViewModel: SessionViewModel,
    onContinue: () -> Unit,
    onAddNext: () -> Unit
) {
    val state by sessionViewModel.state.collectAsStateWithLifecycle()
    val page = state.pages.lastOrNull()
    val imageBitmap = remember(page?.uriPath) {
        page?.uriPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeBgPrimary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Clear Copy", fontWeight = FontWeight.SemiBold, color = SeTextPrimary)
            IconButton(onClick = { sessionViewModel.rotateSelected() }) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = SeGold)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .background(SeBgElevated, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null && page != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Scanned page",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .rotate(page.rotationDeg.toFloat())
                )
            } else {
                Text("No page", color = SeTextSecondary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip("Magic Color", state.activeFilter == ScanFilter.MagicColor) {
                sessionViewModel.setFilter(ScanFilter.MagicColor)
            }
            FilterChip("B&W", state.activeFilter == ScanFilter.BlackWhite) {
                sessionViewModel.setFilter(ScanFilter.BlackWhite)
            }
            FilterChip("Original", state.activeFilter == ScanFilter.Original) {
                sessionViewModel.setFilter(ScanFilter.Original)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (state.mode == ScanMode.Batch) onAddNext() else onContinue()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SeGold, contentColor = SeBgPrimary)
        ) {
            Text(
                if (state.mode == ScanMode.Batch) "Add next page" else "Continue",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, if (selected) SeGold else SeTextSecondary, RoundedCornerShape(999.dp))
            .background(if (selected) SeGold.copy(alpha = 0.15f) else SeBgElevated, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) SeGold else SeTextPrimary, fontWeight = FontWeight.Medium)
    }
}
