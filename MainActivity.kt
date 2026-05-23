package com.zoya.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.zoya.ai.ui.theme.ZoyaTheme
import com.zoya.ai.ui.ZoyaHomeScreen
import kotlinx.coroutines.*
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private val apiClient = ApiClient()

    // UI state
    var isListening = mutableStateOf(false)
    var isSpeaking = mutableStateOf(false)
    var isThinking = mutableStateOf(false)
    var statusText = mutableStateOf("STANDBY")
    var messages = mutableStateListOf<ChatMessage>()
    var apiKey = mutableStateOf("")

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Mic permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecognition()
        else statusText.value = "MIC PERMISSION DENIED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // Add welcome message
        messages.add(
            ChatMessage(
                role = "zoya",
                text = "Hello. I am ZOYA, your AI assistant. Enter your API key in settings, then tap the mic or type a command."
            )
        )

        setContent {
            ZoyaTheme {
                ZoyaHomeScreen(
                    messages = messages,
                    isListening = isListening.value,
                    isSpeaking = isSpeaking.value,
                    isThinking = isThinking.value,
                    statusText = statusText.value,
                    apiKey = apiKey.value,
                    onApiKeyChange = { apiKey.value = it },
                    onMicClick = { toggleListening() },
                    onSendText = { text -> handleUserInput(text) }
                )
            }
        }
    }

    fun toggleListening() {
        if (isListening.value) {
            speechRecognizer.stopListening()
            isListening.value = false
            statusText.value = "STANDBY"
        } else {
            checkMicAndListen()
        }
    }

    private fun checkMicAndListen() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> startVoiceRecognition()

            else -> requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening.value = true
                statusText.value = "LISTENING..."
            }
            override fun onResults(results: Bundle?) {
                isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.get(0) ?: return
                handleUserInput(command)
            }
            override fun onError(error: Int) {
                isListening.value = false
                statusText.value = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "NO MATCH — TRY AGAIN"
                    SpeechRecognizer.ERROR_NETWORK -> "NETWORK ERROR"
                    SpeechRecognizer.ERROR_AUDIO -> "AUDIO ERROR"
                    else -> "RECOGNITION ERROR"
                }
            }
            override fun onBeginningOfSpeech() { statusText.value = "RECEIVING..." }
            override fun onEndOfSpeech() { statusText.value = "PROCESSING..." }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    fun handleUserInput(text: String) {
        if (text.isBlank()) return
        messages.add(ChatMessage(role = "user", text = text))
        processCommand(text)
    }

    private fun processCommand(command: String) {
        val lower = command.lowercase()

        // Built-in device commands (no API needed)
        when {
            lower.contains("open youtube") -> {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (intent != null) {
                    startActivity(intent)
                    respond("Opening YouTube for you.")
                } else respond("YouTube doesn't appear to be installed.")
                return
            }
            lower.contains("open whatsapp") -> {
                val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) {
                    startActivity(intent)
                    respond("Opening WhatsApp.")
                } else respond("WhatsApp doesn't appear to be installed.")
                return
            }
            lower.contains("open camera") -> {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                startActivity(intent)
                respond("Opening camera.")
                return
            }
            lower.contains("open settings") -> {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                respond("Opening device settings.")
                return
            }
            lower.contains("what time") || lower == "time" -> {
                val time = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                respond("The current time is $time.")
                return
            }
            lower.contains("what date") || lower == "date" -> {
                val date = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
                respond("Today is $date.")
                return
            }
        }

        // AI fallback via Claude API
        scope.launch {
            isThinking.value = true
            statusText.value = "THINKING..."

            val key = apiKey.value.trim()
            if (key.isEmpty()) {
                respond("Please enter your Anthropic API key in the settings panel.")
                isThinking.value = false
                return@launch
            }

            val history = messages
                .filter { it.role == "user" || it.role == "zoya" }
                .takeLast(20)
                .map {
                    mapOf(
                        "role" to if (it.role == "user") "user" else "assistant",
                        "content" to it.text
                    )
                }

            val result = apiClient.ask(key, history)
            isThinking.value = false
            respond(result)
        }
    }

    private fun respond(text: String) {
        messages.add(ChatMessage(role = "zoya", text = text))
        speak(text)
    }

    private fun speak(text: String) {
        tts.stop()
        isSpeaking.value = true
        statusText.value = "SPEAKING"
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zoya_utt_${System.currentTimeMillis()}")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            tts.setSpeechRate(0.95f)
            tts.setPitch(0.9f)
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        isSpeaking.value = false
                        statusText.value = "STANDBY"
                    }
                }
                override fun onError(utteranceId: String?) {
                    runOnUiThread { isSpeaking.value = false }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        speechRecognizer.destroy()
        tts.shutdown()
    }
}

data class ChatMessage(val role: String, val text: String)
