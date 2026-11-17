package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.BebasUiState
import com.example.viewmodel.BrowserScreenMode
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldControlSheet(
    uiState: BebasUiState,
    onDismiss: () -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onToggleBackgroundKeepAlive: (Boolean) -> Unit,
    onToggleSessionGuard: (Boolean) -> Unit,
    onToggleForceCopy: (Boolean) -> Unit,
    onToggleAccidentalExitLock: (Boolean) -> Unit,
    onToggleDesktopMode: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onReloadPage: () -> Unit,
    onClearSessionCache: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isWebActive = uiState.screenMode == BrowserScreenMode.ActiveWeb

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Liquid Glass Telemetry Header
            LiquidGlassCard(
                shape = RoundedCornerShape(20.dp),
                highlighted = isWebActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = "Status Perisai StayFlow",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Pusat Kontrol StayFlow",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = if (isWebActive) {
                                    "Bebas matiin layar atau pindah app; timer & progres tetap berjalan"
                                } else {
                                    "Siap menjaga kelangsungan sesi web Anda"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (isWebActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TelemetryStatChip(
                                icon = Icons.Rounded.Timer,
                                label = "Timer Berjalan",
                                value = formatSessionDuration(uiState.sessionSeconds),
                                modifier = Modifier.weight(1f),
                            )
                            TelemetryStatChip(
                                icon = Icons.Rounded.Shield,
                                label = "Gangguan Ditahan",
                                value = "${uiState.shieldedCount}x",
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (!uiState.lastShieldedEvent.isNullOrBlank()) {
                            Text(
                                text = "Terakhir ditahan: ${uiState.lastShieldedEvent}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // Protection Toggles with LiquidSwitch
            ShieldToggleRow(
                icon = Icons.Rounded.AllInclusive,
                title = "Bebas Matiin Layar & Keluar App",
                subtitle = "Tetap bisa matiin layar atau buka WhatsApp; timer web terus jalan dan progres aman",
                checked = uiState.preferences.backgroundKeepAlive,
                onCheckedChange = onToggleBackgroundKeepAlive,
                testTag = "sheet_toggle_background_keep_alive",
            )

            ShieldToggleRow(
                icon = Icons.Rounded.LightMode,
                title = "Layar Tetap Menyala",
                subtitle = "Mencegah layar mati otomatis saat membaca soal",
                checked = uiState.preferences.keepScreenOn,
                onCheckedChange = onToggleKeepScreenOn,
                testTag = "sheet_toggle_keep_screen_on",
            )

            ShieldToggleRow(
                icon = Icons.Rounded.Shield,
                title = "Pelindung Fokus & Visibilitas Web",
                subtitle = "Tahan sinyal keluar tab/blur agar web tidak mendeteksi pelanggaran",
                checked = uiState.preferences.sessionGuardEnabled,
                onCheckedChange = onToggleSessionGuard,
                testTag = "sheet_toggle_session_guard",
            )

            ShieldToggleRow(
                icon = Icons.Rounded.ContentCopy,
                title = "Paksa Salin & Seleksi Teks",
                subtitle = "Aktifkan tekan lama & salin pada halaman web yang menguncinya",
                checked = uiState.preferences.forceCopyEnabled,
                onCheckedChange = onToggleForceCopy,
                testTag = "sheet_toggle_force_copy",
            )

            ShieldToggleRow(
                icon = Icons.Rounded.Lock,
                title = "Konfirmasi Tombol Kembali (2x)",
                subtitle = "Hanya mencegah tombol Back menutup tab ke Beranda secara tak sengaja",
                checked = uiState.preferences.accidentalExitLock,
                onCheckedChange = onToggleAccidentalExitLock,
                testTag = "sheet_toggle_exit_lock",
            )

            ShieldToggleRow(
                icon = Icons.Rounded.DesktopWindows,
                title = "Mode Desktop",
                subtitle = "Muat situs dengan identitas browser desktop",
                checked = uiState.preferences.desktopMode,
                onCheckedChange = onToggleDesktopMode,
                testTag = "sheet_toggle_desktop_mode",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // Theme selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tema Tampilan",
                    style = MaterialTheme.typography.titleSmall,
                )
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
                            Text(text = mode.label)
                        }
                    }
                }
            }

            if (isWebActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            onReloadPage()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sheet_reload_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Muat Ulang")
                    }

                    OutlinedButton(
                        onClick = {
                            onClearSessionCache()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sheet_clear_cache_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bersihkan Cache")
                    }
                }
            }

            Text(
                text = "StayFlow v1.2 • Developer: Sky",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TelemetryStatChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ShieldToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
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
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
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

fun formatSessionDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
