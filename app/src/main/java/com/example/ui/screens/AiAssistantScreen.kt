package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.AiChatContent
import com.example.viewmodel.BebasUiState
import com.example.viewmodel.HomeTab

@Composable
fun AiAssistantScreen(
    uiState: BebasUiState,
    bottomInsetPadding: PaddingValues,
    onDraftChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onSelectModel: (String) -> Unit,
    onOpenSettingsTab: (HomeTab) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 18.dp,
                    bottom = bottomInsetPadding.calculateBottomPadding() + 10.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tanya AI",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = uiState.preferences.aiBaseUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                TextButton(onClick = { onOpenSettingsTab(HomeTab.SETTINGS) }) {
                    Text("Endpoint AI")
                }
            }

            AiChatContent(
                uiState = uiState,
                onDraftChange = onDraftChange,
                onSendMessage = onSendMessage,
                onClearChat = onClearChat,
                onSelectModel = onSelectModel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
