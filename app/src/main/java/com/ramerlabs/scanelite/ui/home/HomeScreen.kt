package com.ramerlabs.scanelite.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramerlabs.scanelite.domain.ScanDocument
import com.ramerlabs.scanelite.ui.components.RamerLabsCredits
import com.ramerlabs.scanelite.ui.library.LibraryViewModel
import com.ramerlabs.scanelite.ui.theme.SeBgPrimary
import com.ramerlabs.scanelite.ui.theme.SeBgSurface
import com.ramerlabs.scanelite.ui.theme.SeGold
import com.ramerlabs.scanelite.ui.theme.SeTextPrimary
import com.ramerlabs.scanelite.ui.theme.SeTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewScan: () -> Unit,
    onSettings: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val docs by viewModel.documents.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SeBgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ScanElite", fontWeight = FontWeight.Bold, color = SeTextPrimary)
                        Text("Crystal-clear scans", color = SeTextSecondary, fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = SeTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SeBgPrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewScan,
                containerColor = SeGold,
                contentColor = SeBgPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Scan")
            }
        }
    ) { padding ->
        if (docs.isEmpty()) {
            EmptyLibrary(padding, onNewScan)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(docs, key = { it.id }) { doc ->
                    DocumentCard(doc)
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(padding: PaddingValues, onNewScan: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.DocumentScanner,
                contentDescription = null,
                tint = SeGold,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Scan your first document", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(
                "Auto-align, enhance, and share in one flow.",
                color = SeTextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )
            Card(
                onClick = onNewScan,
                colors = CardDefaults.cardColors(containerColor = SeGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "New Scan",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontWeight = FontWeight.SemiBold,
                    color = SeBgPrimary
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            RamerLabsCredits()
        }
    }
}

@Composable
private fun DocumentCard(doc: ScanDocument) {
    val date = SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(doc.createdAt))
    Card(
        colors = CardDefaults.cardColors(containerColor = SeBgSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(SeBgPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("${doc.pageCount} pg", color = SeGold, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(doc.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(date, color = SeTextSecondary, fontSize = 12.sp)
        }
    }
}
