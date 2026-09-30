package ai.chatpilot.app.ui.overlay

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.chatpilot.app.model.Language
import ai.chatpilot.app.model.Tone

sealed class OverlayState {
    object Minimized : OverlayState()
    object Expanded : OverlayState()
    object Reading : OverlayState()
    object Generating : OverlayState()
    data class Suggestions(val replies: List<GeneratedReply>) : OverlayState()
    data class Inserted(val text: String) : OverlayState()
    data class Error(val message: String, val canFallbackPaste: Boolean) : OverlayState()
}

data class GeneratedReply(
    val text: String,
    val style: String,
    val intentDescription: String
)

@Composable
fun FloatingOverlayContent(
    state: OverlayState,
    connectedApp: String,
    selectedTone: Tone,
    selectedLanguage: Language,
    transparency: Float,
    onToneSelected: (Tone) -> Unit,
    onLanguageSelected: (Language) -> Unit,
    onReadChatClicked: () -> Unit,
    onReplySelected: (String) -> Unit,
    onMinimizeClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    onDragDelta: (Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        when (state) {
            is OverlayState.Minimized -> {
                // Minimized Floating Bubble
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onReadChatClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "ChatPilot AI",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            else -> {
                // Expanded Floating Window
                Card(
                    modifier = Modifier
                        .width(340.dp)
                        .wrapContentHeight()
                        .shadow(12.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = transparency)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header with Drag & Window Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ChatPilot AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Row {
                                IconButton(onClick = onMinimizeClicked) {
                                    Icon(Icons.Default.Minimize, contentDescription = "Minimize")
                                }
                                IconButton(onClick = onCloseClicked) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        }

                        // App Connection Pill
                        Text(
                            text = "Connected app: $connectedApp",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // State-driven body
                        when (state) {
                            is OverlayState.Expanded -> {
                                Button(
                                    onClick = onReadChatClicked,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("READ CURRENT CHAT")
                                }
                            }
                            is OverlayState.Reading -> {
                                Text("Reading current conversation...")
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            is OverlayState.Generating -> {
                                Text("Creating natural replies...")
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            is OverlayState.Suggestions -> {
                                state.replies.forEachIndexed { index, reply ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onReplySelected(reply.text) }
                                    ) {
                                        Text(text = reply.text, modifier = Modifier.padding(12.dp))
                                    }
                                }
                            }
                            is OverlayState.Inserted -> {
                                Text("Reply inserted ✓\nReview it and press Send.", color = Color.Green)
                            }
                            is OverlayState.Error -> {
                                Text(state.message, color = MaterialTheme.colorScheme.error)
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}