package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.SessionGuardScript
import com.example.data.BookmarkItem
import com.example.data.RecentSession
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.liquidPressScale
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.BebasUiState

@Composable
fun SavedScreen(
    uiState: BebasUiState,
    bottomInsetPadding: PaddingValues,
    onOpenUrl: (String) -> Unit,
    onAddBookmark: (title: String, url: String) -> Unit,
    onRemoveBookmark: (BookmarkItem) -> Unit,
    onRemoveRecentSession: (RecentSession) -> Unit,
    onClearRecentSessions: () -> Unit,
) {
    var selectedSegment by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = bottomInsetPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header", contentType = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Simpanan",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "Tautan cepat dan riwayat sesi web",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (selectedSegment == 0) {
                        FilledTonalButton(
                            onClick = { showAddDialog = true },
                            shape = CircleShape,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .liquidPressScale()
                                .testTag("add_bookmark_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Tambah tautan",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah")
                        }
                    } else if (uiState.preferences.recentSessions.isNotEmpty()) {
                        TextButton(
                            onClick = onClearRecentSessions,
                            modifier = Modifier.testTag("clear_recent_button"),
                        ) {
                            Text("Hapus Semua")
                        }
                    }
                }
            }

            item(key = "segmented", contentType = "segmented") {
                val options = listOf(
                    "Tautan (${uiState.preferences.bookmarks.size})",
                    "Riwayat (${uiState.preferences.recentSessions.size})",
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size,
                            ),
                            selected = selectedSegment == index,
                            onClick = { selectedSegment = index },
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            if (selectedSegment == 0) {
                if (uiState.preferences.bookmarks.isEmpty()) {
                    item(key = "empty_bookmarks", contentType = "empty") {
                        SavedEmptyCard(
                            icon = Icons.Rounded.Bookmark,
                            title = "Belum ada tautan tersimpan",
                            subtitle = "Ketuk + Tambah untuk menyimpan link soal atau portal interview favorit Anda.",
                            onClick = { showAddDialog = true },
                        )
                    }
                } else {
                    items(
                        items = uiState.preferences.bookmarks,
                        key = { "bm_${it.id}" },
                        contentType = { "link_row" },
                    ) { bookmark ->
                        SavedLinkCard(
                            title = bookmark.title,
                            url = bookmark.url,
                            icon = Icons.Rounded.Bookmark,
                            onClick = { onOpenUrl(bookmark.url) },
                            onDeleteClick = { onRemoveBookmark(bookmark) },
                            testTag = "bookmark_item_${bookmark.id}",
                        )
                    }
                }
            } else {
                if (uiState.preferences.recentSessions.isEmpty()) {
                    item(key = "empty_recent", contentType = "empty") {
                        SavedEmptyCard(
                            icon = Icons.Rounded.History,
                            title = "Riwayat sesi masih kosong",
                            subtitle = "Situs web yang Anda buka di StayFlow akan tercatat otomatis di sini.",
                            onClick = {},
                        )
                    }
                } else {
                    items(
                        items = uiState.preferences.recentSessions,
                        key = { "rc_${it.url}" },
                        contentType = { "link_row" },
                    ) { session ->
                        SavedLinkCard(
                            title = session.title,
                            url = session.url,
                            icon = Icons.Rounded.History,
                            onClick = { onOpenUrl(session.url) },
                            onDeleteClick = { onRemoveRecentSession(session) },
                            testTag = "recent_item_${session.url.hashCode()}",
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSavedBookmarkDialog(
            initialUrl = uiState.urlInput,
            onDismiss = { showAddDialog = false },
            onSave = { title, url ->
                onAddBookmark(title, url)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun SavedEmptyCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    LiquidGlassCard(
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SavedLinkCard(
    title: String,
    url: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    testTag: String,
) {
    LiquidGlassCard(
        shape = RoundedCornerShape(18.dp),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = SessionGuardScript.extractHostDisplay(url),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Hapus tautan",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddSavedBookmarkDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (title: String, url: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf(initialUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simpan Tautan Cepat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Tautan (opsional)") },
                    placeholder = { Text("Misal: Soal Interview Kerja") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_bookmark_title"),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Alamat Web (URL)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_bookmark_url"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, url) },
                enabled = url.isNotBlank(),
                modifier = Modifier.testTag("dialog_bookmark_save"),
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}
