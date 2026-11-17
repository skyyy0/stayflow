package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidSwitch
import com.example.ui.components.liquidPressScale
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.BebasUiState

@Composable
fun SettingsScreen(
    uiState: BebasUiState,
    bottomInsetPadding: PaddingValues,
    onToggleBackgroundKeepAlive: (Boolean) -> Unit,
    onToggleSessionGuard: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleForceCopy: (Boolean) -> Unit,
    onToggleAccidentalExitLock: (Boolean) -> Unit,
    onToggleDesktopMode: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSaveAiConfig: (baseUrl: String, model: String) -> Unit,
    onFetchAiModels: () -> Unit,
) {
    var showGuideDialog by remember { mutableStateOf(false) }
    var draftBaseUrl by remember(uiState.preferences.aiBaseUrl) {
        mutableStateOf(uiState.preferences.aiBaseUrl)
    }
    var draftModel by remember(uiState.preferences.aiModel) {
        mutableStateOf(uiState.preferences.aiModel)
    }

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
            item(key = "title", contentType = "header") {
                Column {
                    Text(
                        text = "Perisai & Pengaturan",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Kontrol kontinuitas sesi, salin paksa, tema, dan AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item(key = "section_continuity", contentType = "section_label") {
                SettingsGroupLabel("Kontinuitas & Anti-Blokir")
            }

            item(key = "continuity_cards", contentType = "toggles") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsSwitchCard(
                        icon = Icons.Rounded.AllInclusive,
                        title = "Bebas Matiin Layar & Keluar App",
                        subtitle = "Layanan latar + WakeLock menjaga CPU, jaringan, & timer web tetap jalan saat layar mati atau pindah ke WhatsApp.",
                        checked = uiState.preferences.backgroundKeepAlive,
                        onCheckedChange = onToggleBackgroundKeepAlive,
                        testTag = "home_toggle_background_keep_alive",
                    )
                    SettingsSwitchCard(
                        icon = Icons.Rounded.Shield,
                        title = "Pelindung Fokus & Visibilitas Web",
                        subtitle = "Disuntikkan sejak awal dokumen untuk menahan sinyal visibilitychange, blur, dan pagehide.",
                        checked = uiState.preferences.sessionGuardEnabled,
                        onCheckedChange = onToggleSessionGuard,
                        testTag = "home_toggle_session_guard",
                    )
                    SettingsSwitchCard(
                        icon = Icons.Rounded.LightMode,
                        title = "Layar Tetap Menyala",
                        subtitle = "Mencegah layar mati otomatis saat Anda sedang membaca soal panjang.",
                        checked = uiState.preferences.keepScreenOn,
                        onCheckedChange = onToggleKeepScreenOn,
                        testTag = "home_toggle_keep_screen_on",
                    )
                }
            }

            item(key = "section_browser", contentType = "section_label") {
                SettingsGroupLabel("Interaksi Web & Salin Teks")
            }

            item(key = "browser_cards", contentType = "toggles") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsSwitchCard(
                        icon = Icons.Rounded.ContentCopy,
                        title = "Paksa Aktifkan Salin & Seleksi Teks",
                        subtitle = "Membuka kunci tekan lama, seleksi teks, dan salin pada situs yang memblokir copy-paste.",
                        checked = uiState.preferences.forceCopyEnabled,
                        onCheckedChange = onToggleForceCopy,
                        testTag = "settings_toggle_force_copy",
                    )
                    SettingsSwitchCard(
                        icon = Icons.Rounded.Lock,
                        title = "Konfirmasi Tombol Kembali (2x)",
                        subtitle = "Mencegah tombol Back menutup tab ke Beranda secara tidak sengaja.",
                        checked = uiState.preferences.accidentalExitLock,
                        onCheckedChange = onToggleAccidentalExitLock,
                        testTag = "home_toggle_exit_lock",
                    )
                    SettingsSwitchCard(
                        icon = Icons.Rounded.DesktopWindows,
                        title = "Mode Desktop Default",
                        subtitle = "Buka halaman web dengan identitas browser desktop.",
                        checked = uiState.preferences.desktopMode,
                        onCheckedChange = onToggleDesktopMode,
                        testTag = "settings_toggle_desktop_mode",
                    )
                }
            }

            item(key = "section_theme", contentType = "section_label") {
                SettingsGroupLabel("Tema Liquid Glass")
            }

            item(key = "theme_card", contentType = "theme") {
                LiquidGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = ThemeMode.entries.size,
                                    ),
                                    selected = uiState.preferences.themeMode == mode,
                                    onClick = { onSelectThemeMode(mode) },
                                    modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}"),
                                ) {
                                    Text(mode.label)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "section_ai", contentType = "section_label") {
                SettingsGroupLabel("Endpoint Asisten AI")
            }

            item(key = "ai_config_card", contentType = "ai_config") {
                LiquidGlassCard(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column {
                                Text(
                                    text = "Konfigurasi Endpoint OpenAI-Kompatibel",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "API key dibaca aman dari Secrets (XINGCHI_API_KEY).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        OutlinedTextField(
                            value = draftBaseUrl,
                            onValueChange = { draftBaseUrl = it },
                            label = { Text("Base URL (/v1)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_ai_base_url"),
                        )

                        OutlinedTextField(
                            value = draftModel,
                            onValueChange = { draftModel = it },
                            label = { Text("Model AI") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_ai_model"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    onSaveAiConfig(draftBaseUrl, draftModel)
                                    onFetchAiModels()
                                },
                                enabled = !uiState.isFetchingModels,
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .liquidPressScale(),
                            ) {
                                if (uiState.isFetchingModels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudSync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cek /v1/models")
                            }

                            Button(
                                onClick = { onSaveAiConfig(draftBaseUrl, draftModel) },
                                shape = CircleShape,
                                modifier = Modifier
                                    .weight(1f)
                                    .liquidPressScale()
                                    .testTag("settings_save_ai_button"),
                            ) {
                                Text("Simpan")
                            }
                        }

                        if (!uiState.aiModelStatusMessage.isNullOrBlank()) {
                            Text(
                                text = uiState.aiModelStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            item(key = "about_sky", contentType = "footer") {
                LiquidGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "StayFlow • v1.2",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Developer: Sky",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        FilledTonalButton(
                            onClick = { showGuideDialog = true },
                            shape = CircleShape,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .liquidPressScale()
                                .testTag("open_guide_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cara Kerja")
                        }
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = { Text("Cara Kerja StayFlow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. Bebas Matiin Layar & Keluar App: Layanan latar + WakeLock menjaga CPU, WebSocket, dan timer JS tetap hidup walau layar mati atau Anda membalas chat di WhatsApp.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "2. Paksa Salin Teks & Tanya AI: Situs yang mematikan klik lama/seleksi teks otomatis dibuka kembali oleh StayFlow, sehingga Anda bisa langsung menyalin soal dan bertanya ke Asisten AI.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Dikembangkan oleh Sky.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showGuideDialog = false }) {
                    Text("Mengerti")
                }
            },
        )
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    LiquidGlassCard(
        shape = RoundedCornerShape(20.dp),
        highlighted = checked,
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (checked) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(21.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LiquidSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag),
            )
        }
    }
}
