package com.zoya.ai.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.ai.ChatMessage
import kotlinx.coroutines.launch

val Cyan = Color(0xFF00F5FF)
val DeepBlue = Color(0xFF0080FF)
val Background = Color(0xFF020412)
val PanelBg = Color(0xFF07182E)
val DimColor = Color(0xFF4A7A9B)
val TextColor = Color(0xFFC8EEFF)

@Composable
fun ZoyaHomeScreen(
    messages: List<ChatMessage>,
    isListening: Boolean,
    isSpeaking: Boolean,
    isThinking: Boolean,
    statusText: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onSendText: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Background grid effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = androidx.compose.ui.graphics.Color(0x05004466)
            val spacing = 40.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f)
                x += spacing
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f)
                y += spacing
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Title ──
            Text(
                text = "ZOYA",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 38.sp,
                color = Cyan,
                letterSpacing = 6.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "VOICE AI ASSISTANT",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = DimColor,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(12.dp))

            // ── Orb ──
            ZoyaOrb(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isThinking = isThinking
            )

            Spacer(Modifier.height(4.dp))

            // ── Status ──
            Text(
                text = statusText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (isListening || isSpeaking || isThinking) Cyan else DimColor,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(10.dp))

            // ── API Key ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PanelBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0x1500F5FF))
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        text = "⚡ ANTHROPIC API KEY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = DimColor,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("sk-ant-...", fontSize = 11.sp, color = DimColor,
                                fontFamily = FontFamily.Monospace)
                        },
                        visualTransformation = if (showApiKey) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextColor, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color(0x2200F5FF),
                            cursorColor = Cyan
                        ),
                        trailingIcon = {
                            TextButton(onClick = { showApiKey = !showApiKey }) {
                                Text(
                                    if (showApiKey) "HIDE" else "SHOW",
                                    fontSize = 8.sp, color = DimColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Chat Log ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = PanelBg.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0x1100F5FF))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg)
                    }
                    if (isThinking) {
                        item {
                            ThinkingBubble()
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Mic Button ──
            MicButton(
                isListening = isListening,
                onClick = onMicClick
            )

            Spacer(Modifier.height(10.dp))

            // ── Text Input ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Type a command...", fontSize = 11.sp, color = DimColor,
                            fontFamily = FontFamily.Monospace)
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextColor, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color(0x2200F5FF),
                        cursorColor = Cyan
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.isNotBlank()) {
                            onSendText(textInput)
                            textInput = ""
                            focusManager.clearFocus()
                        }
                    })
                )
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendText(textInput)
                            textInput = ""
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.radialGradient(listOf(Cyan.copy(0.15f), Color.Transparent)),
                            CircleShape
                        )
                        .border(1.dp, Cyan.copy(0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Cyan)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ZoyaOrb(isListening: Boolean, isSpeaking: Boolean, isThinking: Boolean) {
    val ring1Rotation = rememberInfiniteTransition(label = "r1")
        .animateFloat(0f, 360f, InfiniteRepeatableSpec(tween(4000, easing = LinearEasing)), "r1")
    val ring2Rotation = rememberInfiniteTransition(label = "r2")
        .animateFloat(360f, 0f, InfiniteRepeatableSpec(tween(6000, easing = LinearEasing)), "r2")
    val ring3Rotation = rememberInfiniteTransition(label = "r3")
        .animateFloat(0f, 360f, InfiniteRepeatableSpec(tween(9000, easing = LinearEasing)), "r3")

    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = if (isListening || isSpeaking) 1.08f else 1.02f,
        animationSpec = InfiniteRepeatableSpec(
            tween(if (isSpeaking) 500 else 1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "pulse"
    )

    Box(
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ring 1
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(ring1Rotation.value)
                .border(
                    1.5.dp,
                    Brush.sweepGradient(listOf(Cyan, Color.Transparent, Cyan.copy(0.3f))),
                    CircleShape
                )
        )
        // Ring 2
        Box(
            modifier = Modifier
                .size(162.dp)
                .rotate(ring2Rotation.value)
                .border(
                    1.dp,
                    Brush.sweepGradient(listOf(DeepBlue, Color.Transparent, DeepBlue.copy(0.3f))),
                    CircleShape
                )
        )
        // Ring 3
        Box(
            modifier = Modifier
                .size(134.dp)
                .rotate(ring3Rotation.value)
                .border(
                    1.dp,
                    Brush.sweepGradient(listOf(Cyan.copy(0.5f), Color.Transparent)),
                    CircleShape
                )
        )
        // Core
        Box(
            modifier = Modifier
                .size((104 * pulseScale).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            when {
                                isListening -> Cyan.copy(alpha = 0.35f)
                                isSpeaking -> DeepBlue.copy(alpha = 0.35f)
                                isThinking -> Cyan.copy(alpha = 0.2f)
                                else -> Cyan.copy(alpha = 0.12f)
                            },
                            PanelBg
                        )
                    )
                )
                .border(
                    1.5.dp,
                    if (isListening || isSpeaking) Cyan else Cyan.copy(0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ZOYA",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Cyan,
                letterSpacing = 3.sp
            )
        }
    }
}

@Composable
fun MicButton(isListening: Boolean, onClick: () -> Unit) {
    val scale by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f, targetValue = if (isListening) 1.1f else 1f,
        animationSpec = InfiniteRepeatableSpec(tween(900), RepeatMode.Reverse),
        label = "micScale"
    )

    Box(
        modifier = Modifier
            .size((60 * scale).dp)
            .clip(CircleShape)
            .background(
                if (isListening)
                    Brush.radialGradient(listOf(Cyan.copy(0.3f), Cyan.copy(0.05f)))
                else
                    Brush.radialGradient(listOf(Cyan.copy(0.12f), Color.Transparent))
            )
            .border(
                2.dp,
                if (isListening) Cyan else Cyan.copy(0.4f),
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = "Mic",
            tint = Cyan,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Text(
                text = if (isUser) "YOU▸" else "ZOYA▸",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = if (isUser) DeepBlue else Cyan,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
            Surface(
                color = if (isUser) Color(0x1A0080FF) else Color(0x1400F5FF),
                shape = RoundedCornerShape(
                    topStart = if (isUser) 12.dp else 2.dp,
                    topEnd = if (isUser) 2.dp else 12.dp,
                    bottomStart = 12.dp, bottomEnd = 12.dp
                ),
                border = BorderStroke(
                    0.5.dp,
                    if (isUser) DeepBlue.copy(0.3f) else Cyan.copy(0.2f)
                )
            ) {
                Text(
                    text = message.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = if (isUser) TextColor else Color(0xFFE0F8FF),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun ThinkingBubble() {
    val dots = remember { listOf("▪", "▪", "▪") }
    val alpha1 by rememberInfiniteTransition(label = "d1").animateFloat(
        0.2f, 1f, InfiniteRepeatableSpec(tween(600), RepeatMode.Reverse), "d1"
    )
    val alpha2 by rememberInfiniteTransition(label = "d2").animateFloat(
        0.2f, 1f, InfiniteRepeatableSpec(tween(600, delayMillis = 200), RepeatMode.Reverse), "d2"
    )
    val alpha3 by rememberInfiniteTransition(label = "d3").animateFloat(
        0.2f, 1f, InfiniteRepeatableSpec(tween(600, delayMillis = 400), RepeatMode.Reverse), "d3"
    )

    Row(Modifier.fillMaxWidth()) {
        Column {
            Text("ZOYA▸", fontFamily = FontFamily.Monospace, fontSize = 8.sp,
                color = Cyan, letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
            Surface(
                color = Color(0x1400F5FF),
                shape = RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                border = BorderStroke(0.5.dp, Cyan.copy(0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("▪", color = Cyan.copy(alpha1), fontSize = 14.sp)
                    Text("▪", color = Cyan.copy(alpha2), fontSize = 14.sp)
                    Text("▪", color = Cyan.copy(alpha3), fontSize = 14.sp)
                }
            }
        }
    }
}
