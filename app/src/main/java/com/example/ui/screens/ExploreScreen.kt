package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.browser.SessionGuardScript
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.liquidPressScale
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.BebasUiState
import com.example.viewmodel.HomeTab

@Composable
fun ExploreScreen(
    uiState: BebasUiState,
    bottomInsetPadding: PaddingValues,
    onUrlInputChange: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onJumpToTab: (HomeTab) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

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
                top = 16.dp,
                bottom = bottomInsetPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hero", contentType = "hero") {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(initialOffsetY = { -24 }),
                ) {
                    ExploreHeroCard(
                        onOpenShieldTab = { onJumpToTab(HomeTab.SETTINGS) },
                    )
                }
            }

            item(key = "url_launcher", contentType = "launcher") {
                LiquidGlassCard(
                    shape = RoundedCornerShape(24.dp),
                    highlighted = uiState.urlInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Buka Situs Tujuan",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OutlinedTextField(
                            value = uiState.urlInput,
                            onValueChange = onUrlInputChange,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = "https://contoh-web.com atau cari…",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingIcon = {
                                if (uiState.urlInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onUrlInputChange("") },
                                        modifier = Modifier.testTag("clear_url_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Hapus URL",
                                        )
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go,
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if (uiState.urlInput.isNotBlank()) onOpenUrl(uiState.urlInput)
                                },
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_url_input"),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text?.trim()
                                    if (!clip.isNullOrEmpty()) onUrlInputChange(clip)
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(50.dp)
                                    .liquidPressScale()
                                    .testTag("paste_url_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Tempel link",
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tempel")
                            }

                            Button(
                                onClick = {
                                    if (uiState.urlInput.isNotBlank()) onOpenUrl(uiState.urlInput)
                                },
                                enabled = uiState.urlInput.isNotBlank(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .liquidPressScale(enabled = uiState.urlInput.isNotBlank())
                                    .testTag("open_web_button"),
                            ) {
                                Text("Buka Web")
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Live Status Summary Pills Grid
            item(key = "shield_summary", contentType = "summary") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Status Perlindungan Aktif",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatusFeatureMiniCard(
                            icon = Icons.Rounded.AllInclusive,
                            title = "Bebas Keluar",
                            active = uiState.preferences.backgroundKeepAlive,
                            onClick = { onJumpToTab(HomeTab.SETTINGS) },
                            modifier = Modifier.weight(1f),
                        )
                        StatusFeatureMiniCard(
                            icon = Icons.Rounded.Shield,
                            title = "Anti-Blokir",
                            active = uiState.preferences.sessionGuardEnabled,
                            onClick = { onJumpToTab(HomeTab.SETTINGS) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatusFeatureMiniCard(
                            icon = Icons.Rounded.ContentCopy,
                            title = "Paksa Salin",
                            active = uiState.preferences.forceCopyEnabled,
                            onClick = { onJumpToTab(HomeTab.SETTINGS) },
                            modifier = Modifier.weight(1f),
                        )
                        StatusFeatureMiniCard(
                            icon = Icons.Rounded.LightMode,
                            title = "Layar Nyala",
                            active = uiState.preferences.keepScreenOn,
                            onClick = { onJumpToTab(HomeTab.SETTINGS) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Quick AI Shortcut Card
            item(key = "ai_shortcut", contentType = "shortcut") {
                LiquidGlassCard(
                    shape = RoundedCornerShape(20.dp),
                    highlighted = true,
                    onClick = { onJumpToTab(HomeTab.AI) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Asisten AI Soal & Kuis",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Bingung jawab soal? Tanyakan ke AI langsung di aplikasi atau di atas halaman web.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Buka Asisten AI",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Quick Resume Last Session (if exists)
            val lastSession = uiState.preferences.recentSessions.firstOrNull()
            if (lastSession != null) {
                item(key = "last_session", contentType = "shortcut") {
                    LiquidGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onOpenUrl(lastSession.url) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
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
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lanjutkan Terakhir",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    text = lastSession.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = SessionGuardScript.extractHostDisplay(lastSession.url),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreHeroCard(onOpenShieldTab: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                    ),
                ),
                shape = RoundedCornerShape(26.dp),
            ),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_shield_hero),
            contentDescription = "Ilustrasi Perisai StayFlow",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x9A0B1118), Color(0xEB0B1118)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                onClick = onOpenShieldTab,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Liquid Shield • Developer: Sky",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "StayFlow",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    text = "Bebas matiin layar atau keluar balas chat penting—timer web tetap jalan normal, fitur salin teks aktif, dan progres aman tanpa terblokir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE2E8F0),
                )
            }
        }
    }
}

@Composable
private fun StatusFeatureMiniCard(
    icon: ImageVector,
    title: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        shape = RoundedCornerShape(18.dp),
        highlighted = active,
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (active) {
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
                    tint = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (active) "Aktif" else "Nonaktif",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
