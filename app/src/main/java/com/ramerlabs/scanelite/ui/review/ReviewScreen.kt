package com.ramerlabs.scanelite.ui.review

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramerlabs.scanelite.ui.session.SessionViewModel
import com.ramerlabs.scanelite.ui.theme.SeBgElevated
import com.ramerlabs.scanelite.ui.theme.SeBgPrimary
import com.ramerlabs.scanelite.ui.theme.SeDanger
import com.ramerlabs.scanelite.ui.theme.SeGold
import com.ramerlabs.scanelite.ui.theme.SeTextPrimary
import com.ramerlabs.scanelite.ui.theme.SeTextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ReviewScreen(
    sessionViewModel: SessionViewModel,
    onShare: () -> Unit,
    onSaveOnly: () -> Unit
) {
    val state by sessionViewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SeBgPrimary)
            .padding(16.dp)
    ) {
        Text("Review", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = SeTextPrimary)
        Text("${state.pages.size} pages", color = SeTextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.documentTitle,
            onValueChange = sessionViewModel::setTitle,
            label = { Text("Document name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SeGold,
                focusedLabelColor = SeGold,
                cursorColor = SeGold,
                focusedTextColor = SeTextPrimary,
                unfocusedTextColor = SeTextPrimary
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(state.pages, key = { _, p -> p.id }) { index, page ->
                val bmp = remember(page.uriPath) {
                    BitmapFactory.decodeFile(page.uriPath)?.asImageBitmap()
                }
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SeBgElevated)
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SeBgPrimary)
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Page ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text(
                            "${index + 1}",
                            color = SeGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = {
                            sessionViewModel.selectPage(page.id)
                            sessionViewModel.rotateSelected()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, null, tint = SeGold)
                        }
                        IconButton(onClick = { sessionViewModel.removePage(page.id) }) {
                            Icon(Icons.Outlined.Delete, null, tint = SeDanger)
                        }
                    }
                    if (index > 0) {
                        Text(
                            "Move up",
                            color = SeTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable { sessionViewModel.movePage(index, index - 1) }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        sessionViewModel.finalizeDocument()
                        onSaveOnly()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SeBgElevated, contentColor = SeTextPrimary)
            ) { Text("Save") }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SeGold, contentColor = SeBgPrimary)
            ) { Text("Crop & export", fontWeight = FontWeight.SemiBold) }
        }
    }
}
