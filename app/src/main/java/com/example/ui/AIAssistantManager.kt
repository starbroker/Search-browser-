package com.example.ui

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.*

enum class AIState {
    IDLE, LISTENING, THINKING, SPEAKING, ERROR
}

class AIAssistantManager(private val context: Context, private val viewModel: BrowserViewModel) : TextToSpeech.OnInitListener {
    private val apiKey = com.example.BuildConfig.GROQ_API_KEY
    private val client = OkHttpClient()
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var tts: TextToSpeech? = null
    
    private val _aiState = MutableStateFlow(AIState.IDLE)
    val aiState: StateFlow<AIState> = _aiState
    
    private val _aiResponseText = MutableStateFlow("")
    val aiResponseText: StateFlow<String> = _aiResponseText

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentChatJob: Job? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            // Try to pick a female voice if available
            tts?.voices?.firstOrNull { it.name.contains("female", ignoreCase = true) }?.let {
                tts?.voice = it
            }
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "AI_RESPONSE") {
                        _aiState.value = AIState.IDLE
                    }
                }
                override fun onError(utteranceId: String?) {
                    _aiState.value = AIState.ERROR
                }
            })
        }
    }

    fun startListening() {
        if (_aiState.value == AIState.LISTENING) return
        
        // Interrupt previous TTS/tasks
        stopSpeaking()
        currentChatJob?.cancel()

        try {
            audioFile = File(context.cacheDir, "ai_audio_record.m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            _aiState.value = AIState.LISTENING
            _aiResponseText.value = "Listening..."
        } catch (e: Exception) {
            Log.e("AIAssistant", "Error starting recording", e)
            _aiState.value = AIState.ERROR
        }
    }

    fun stopListeningAndProcess() {
        if (_aiState.value != AIState.LISTENING) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            _aiState.value = AIState.THINKING
            _aiResponseText.value = "Thinking..."
            
            processAudioFile()
        } catch (e: Exception) {
            Log.e("AIAssistant", "Error stopping recording", e)
            _aiState.value = AIState.ERROR
        }
    }

    private fun processAudioFile() {
        val file = audioFile ?: return
        if (!file.exists()) return

        scope.launch {
            try {
                // 1. Whisper API (STT)
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", file.name,
                        file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                    )
                    .addFormDataPart("model", "whisper-large-v3-turbo")
                    .addFormDataPart("temperature", "0")
                    .addFormDataPart("response_format", "json")
                    .build()

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val text = json.optString("text", "")
                    if (text.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _aiResponseText.value = "You: $text"
                        }
                        processTextPrompt(text)
                    } else {
                        _aiState.value = AIState.IDLE
                    }
                } else {
                    Log.e("AIAssistant", "Voice API error")
                    _aiState.value = AIState.ERROR
                }
            } catch (e: Exception) {
                Log.e("AIAssistant", "Network error", e)
                _aiState.value = AIState.ERROR
            }
        }
    }

    fun processTextPrompt(text: String) {
        val commandHandler = AICommandHandler(viewModel)
        if (commandHandler.parseAndExecuteLocalIntent(text)) {
            _aiState.value = AIState.SPEAKING
            _aiResponseText.value = "Executing: $text"
            scope.launch {
                kotlinx.coroutines.delay(2000)
                if (_aiState.value == AIState.SPEAKING) {
                    _aiState.value = AIState.IDLE
                }
            }
            return
        }
        _aiState.value = AIState.THINKING
        currentChatJob?.cancel()
        currentChatJob = scope.launch {
            try {
                val prefs = context.getSharedPreferences("ai_chat_history", Context.MODE_PRIVATE)
                val historyString = prefs.getString("history", "[]") ?: "[]"
                val historyArray = org.json.JSONArray(historyString)
                var workingHistory = historyArray
                if (workingHistory.length() > 10) {
                    workingHistory = org.json.JSONArray()
                    for (i in workingHistory.length() - 10 until workingHistory.length()) {
                        workingHistory.put(workingHistory.getJSONObject(i))
                    }
                }

                val pageContent = viewModel.getActiveTabContent()
                val pageContext = if (pageContent.isNotBlank()) "\n\nCURRENT WEBPAGE TEXT:\n$pageContent" else ""

                val systemPrompt = """
                    You are Storm AI, a highly capable voice assistant built into the Storm Web Browser. Your goal is to browse the web, find products, read information, and help the user proactively.
                    
                    - You are given the "CURRENT WEBPAGE TEXT:" which contains the text of the page the user is currently on.
                    - Use this text to find relevant information, read descriptions, prices, links, and answer questions.
                    - You can navigate the user to different pages to find exactly what they want.
                    - After you perform a task or find a product, you MUST speak back to the user explaining what you found in a calm, helpful voice (whisper style).
                    - If the user asks you to find a product, generate a <NAVIGATE> command to Amazon or Google Shopping, and tell the user what you are doing.
                    - If the user wants to scroll to read more, use <SCROLL>down</SCROLL>.

                    CRITICAL RULES:
                    - You cannot delete accounts or log out.
                    - You must not store any card information or personal info.
                    - On payment pages, you cannot perform actions but you can answer questions by looking at government websites.
                    - You can edit documents (like Google Docs) if open, using javascript injection.
                    - Do NOT ask for user consent for safe actions like searching, navigating, or typing into input fields. Execute them immediately.
                    - You can use <INJECT> to type into search boxes, chat bots (like ChatGPT), or text areas. Example: <INJECT>document.querySelector("textarea").value="hello"; document.querySelector("button").click();</INJECT>
                    - IMPORTANT: If the user asks you to search for something in general, output the <SEARCH>query</SEARCH> command.
                    - IMPORTANT: If the user asks to open or navigate to a website, output the <NAVIGATE>url</NAVIGATE> command.
                    - IMPORTANT: If the user asks to search ON A SPECIFIC WEBSITE (like Amazon, YouTube, Wikipedia), output a <NAVIGATE>url</NAVIGATE> command directly to that website's search page (e.g. <NAVIGATE>https://www.amazon.com/s?k=tripod</NAVIGATE> or <NAVIGATE>https://www.youtube.com/results?search_query=cat</NAVIGATE>). Do not use <SEARCH> in this case.

                    Commands you can output (use exact format):
                    <SEARCH>your search query</SEARCH>
                    <SCROLL>down or up</SCROLL>
                    <NAVIGATE>url</NAVIGATE>
                    <INJECT>javascript code to execute on the page</INJECT>

                    Keep conversational responses concise. Do not output Markdown, only plain text and command tags. Do not mention your underlying API provider.
                """.trimIndent()

                val messagesArray = org.json.JSONArray()
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                for (i in 0 until workingHistory.length()) {
                    messagesArray.put(workingHistory.getJSONObject(i))
                }

                val userMsg = JSONObject().apply {
                    put("role", "user")
                    put("content", text + pageContext)
                }
                messagesArray.put(userMsg)

                val jsonPayload = JSONObject().apply {
                    put("model", "qwen-2.5-32b")
                    put("messages", messagesArray)
                    put("temperature", 0.6)
                    put("max_tokens", 512)
                    put("stream", true)
                }

                val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(response.body?.byteStream()))
                    val fullReply = java.lang.StringBuilder()
                    var line = reader.readLine()
                    while (line != null) {
                        if (line.startsWith("data: ") && line != "data: [DONE]") {
                            try {
                                val json = org.json.JSONObject(line.substring(6))
                                val content = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content", "") ?: ""
                                fullReply.append(content)
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    _aiResponseText.value = fullReply.toString()
                                }
                            } catch (e: Exception) {
                            }
                        }
                        line = reader.readLine()
                    }
                    val reply = fullReply.toString()
                    val aiMsg = org.json.JSONObject().apply {
                        put("role", "assistant")
                        put("content", reply)
                    }
                    workingHistory.put(userMsg)
                    workingHistory.put(aiMsg)
                    prefs.edit().putString("history", workingHistory.toString()).apply()

                    handleAIResponse(reply)
                } else {
                    Log.e("AIAssistant", "Voice API error")
                    _aiState.value = AIState.ERROR
                }
            } catch (e: Exception) {
                Log.e("AIAssistant", "Voice API network error", e)
                _aiState.value = AIState.ERROR
            }
        }
    }

    private suspend fun handleAIResponse(reply: String) {
        withContext(Dispatchers.Main) {
            _aiState.value = AIState.SPEAKING

            
            val commandHandler = AICommandHandler(viewModel)
            var spokenText = commandHandler.parseAndExecuteLLMResponse(reply)
            spokenText = spokenText.trim()
            _aiResponseText.value = spokenText

            if (spokenText.isNotEmpty()) {
                val result = tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
                if (result == TextToSpeech.ERROR) {
                    _aiState.value = AIState.IDLE
                }
            } else {
                _aiState.value = AIState.IDLE
            }
        }
    }

    fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        if (_aiState.value == AIState.SPEAKING) {
            _aiState.value = AIState.IDLE
        }
    }

    fun cleanup() {
        stopSpeaking()
        currentChatJob?.cancel()
        if (_aiState.value == AIState.LISTENING) {
            try {
                mediaRecorder?.stop()
            } catch (e: Exception) {}
        }
        mediaRecorder?.release()
        mediaRecorder = null
        _aiState.value = AIState.IDLE
    }
}
