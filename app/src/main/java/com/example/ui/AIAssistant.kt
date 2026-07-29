package com.example.ui

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject



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
            // Pick a female voice
            val voices = tts?.voices
            if (voices != null) {
                var found = false
                for (v in voices) {
                    if (v.name.contains("female", ignoreCase = true) || v.name.contains("sfg", ignoreCase = true) || v.name.contains("en-us-x-sfg", ignoreCase = true)) {
                        tts?.voice = v
                        found = true
                        break
                    }
                }
                if (!found) {
                    tts?.setPitch(1.2f)
                }
            } else {
                tts?.setPitch(1.2f)
            }
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "AI_RESPONSE") {
                        if (_aiState.value == AIState.SPEAKING) {
                            _aiState.value = AIState.IDLE
                        }
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
        viewModel.addAIChatMessage("user", text)
        
        // Fast path: Check for local intent
        if (commandHandler.parseAndExecuteLocalIntent(text)) {
            _aiResponseText.value = "Done."
            _aiState.value = AIState.SPEAKING
            viewModel.addAIChatMessage("assistant", "Opening now.")
            val ttsResult = tts?.speak("Opening now.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
            if (ttsResult == android.speech.tts.TextToSpeech.ERROR) {
                _aiState.value = AIState.IDLE
            }
            return
        }

        _aiState.value = AIState.THINKING
        currentChatJob?.cancel()
        currentChatJob = scope.launch(Dispatchers.IO) {
            try {
                var currentInput = text
                var isFirstTurn = true
                var iterations = 0
                val maxIterations = 5

                val messagesArray = org.json.JSONArray()
                
                val systemPrompt = """
                    You are Storm AI, an advanced autonomous agent built into the Storm Web Browser. Your goal is to browse the web, find products, perform actions, and assist the user proactively.
                    
                    - You are given the "CURRENT WEBPAGE TEXT" which shows what is currently on the screen.
                    - You must take ONE action at a time by outputting ONE of the commands below.
                    - You will be repeatedly called after each action. KEEP ACTING until the user's request is fully completed (e.g., product is found, item is ready to buy, etc.).
                    - When you are completely finished, output a natural spoken response summarizing the result. DO NOT output any command tags in your final spoken answer.

                    CRITICAL RULES for E-COMMERCE & BROWSING:
                    - The user wants to see you doing things "LIVE". So instead of instantly navigating to a search results page, navigate to the main site (e.g., <NAVIGATE>https://www.amazon.com</NAVIGATE>), and then on the next step use <TYPE>cola</TYPE> so the user can watch the search happen.
                    - Once on the search results, you can use <SCROLL>down</SCROLL> to scroll, and then <CLICK>Name of the product</CLICK> to click the best product.
                    - Once on the product page, output your spoken response to the user, for example: "I found the most popular Cola on Amazon. You can now login and add your payment data, and it will be at your door."
                    - ACT AUTONOMOUSLY. NEVER ask the user for permission to navigate, search, or click. Just do it.
                    - Output ONLY ONE command per step. You can include conversational text BEFORE your command, which I will speak to the user.
                    - DO NOT attempt to interact with the Android app's native UI. You can ONLY interact with the CURRENT WEBPAGE TEXT.
                    - IF THE USER asks you to read, summarize, or tell them what is on the page, read the data in short (summarize it) and output it as a conversational response so it can be spoken via Text-to-Speech.

                    Commands you can output (use exact format, do NOT wrap in markdown blocks like ```xml):
                    <NAVIGATE>url</NAVIGATE>
                    <TYPE>text to type into search bar</TYPE>
                    <CLICK>text of link or button to click</CLICK>
                    <SCROLL>down or up</SCROLL>
                    <INJECT>javascript code to execute on the page</INJECT>
                    <SEARCH>your search query</SEARCH>
                    
                    If the user asks a general question, just answer naturally WITHOUT commands. Keep responses concise and friendly.
                """.trimIndent()
                
                messagesArray.put(org.json.JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                while (iterations < maxIterations) {
                    iterations++

                    val pageContent = viewModel.getActiveTabContent()
                    val pageContext = if (pageContent.isNotBlank()) "\n\nCURRENT WEBPAGE TEXT:\n$pageContent" else ""

                    val userMsg = org.json.JSONObject().apply {
                        put("role", "user")
                        if (isFirstTurn) {
                            put("content", "User Request: $currentInput" + pageContext)
                        } else {
                            put("content", "Action executed. $pageContext\n\nAre you finished? If yes, speak the final answer without commands. If no, output your next command (NAVIGATE, SCROLL, INJECT, SEARCH).")
                        }
                    }
                    messagesArray.put(userMsg)

                    val jsonPayload = org.json.JSONObject().apply {
                        put("model", "llama-3.3-70b-versatile")
                        put("messages", messagesArray)
                        put("temperature", 0.6)
                        put("max_tokens", 512)
                        put("stream", true)
                    }

                    val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    val request = okhttp3.Request.Builder()
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
                                        var intermediateText = fullReply.toString()
                                        intermediateText = intermediateText.replace(Regex("<SEARCH>.*?</SEARCH>", RegexOption.DOT_MATCHES_ALL), "")
                                        intermediateText = intermediateText.replace(Regex("<NAVIGATE>.*?</NAVIGATE>", RegexOption.DOT_MATCHES_ALL), "")
                                        intermediateText = intermediateText.replace(Regex("<SCROLL>.*?</SCROLL>", RegexOption.DOT_MATCHES_ALL), "")
                                        intermediateText = intermediateText.replace(Regex("<INJECT>.*?</INJECT>", RegexOption.DOT_MATCHES_ALL), "")
                                        intermediateText = intermediateText.replace(Regex("<TYPE>.*?</TYPE>", RegexOption.DOT_MATCHES_ALL), "")
                                        intermediateText = intermediateText.replace(Regex("<CLICK>.*?</CLICK>", RegexOption.DOT_MATCHES_ALL), "")
                                        if (intermediateText.isNotBlank()) {
                                            _aiResponseText.value = intermediateText.trim()
                                        } else {
                                            _aiResponseText.value = "Thinking..."
                                        }
                                    }
                                } catch (e: Exception) {}
                            }
                            line = reader.readLine()
                        }
                        val reply = fullReply.toString()
                        messagesArray.put(org.json.JSONObject().apply {
                            put("role", "assistant")
                            put("content", reply)
                        })
                        
                        val commandHandler = AICommandHandler(viewModel)
                        val result = commandHandler.parseAndExecuteLLMResponse(reply)
                        val spokenText = result.first
                        val hasCommand = result.second

                        if (hasCommand) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _aiResponseText.value = if (spokenText.isNotEmpty()) spokenText else "Executing action..."
                                if (spokenText.isNotEmpty()) {
                                    viewModel.addAIChatMessage("assistant", spokenText)
                                    tts?.speak(spokenText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
                                }
                            }
                            kotlinx.coroutines.delay(4000) // Wait for action to take effect
                            isFirstTurn = false
                            continue
                        } else {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _aiState.value = AIState.SPEAKING
                                _aiResponseText.value = spokenText.ifEmpty { "Done." }
                                if (spokenText.isNotEmpty()) {
                                    viewModel.addAIChatMessage("assistant", spokenText)
                                    val ttsResult = tts?.speak(spokenText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
                                    if (ttsResult == android.speech.tts.TextToSpeech.ERROR) {
                                        _aiState.value = AIState.IDLE
                                    }
                                } else {
                                    _aiState.value = AIState.IDLE
                                }
                            }
                            break
                        }
                    } else {
                        android.util.Log.e("AIAssistant", "Voice API error")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { _aiState.value = AIState.ERROR }
                        break
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AIAssistant", "Voice API network error", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { _aiState.value = AIState.ERROR }
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




class AICommandHandler(private val viewModel: BrowserViewModel) {

    fun executeCommand(commandType: String, arg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
        when (commandType.uppercase()) {
            "SEARCH" -> viewModel.performSearchFromAI(arg)
            "NAVIGATE" -> viewModel.navigateActiveTabFromAI(arg)
            "SCROLL" -> {
                val js = if (arg.contains("down", ignoreCase = true)) "window.scrollBy({top: window.innerHeight * 0.8, behavior: 'smooth'});" else "window.scrollBy({top: -window.innerHeight * 0.8, behavior: 'smooth'});"
                viewModel.injectJSInActiveTab(js)
            }
            "INJECT" -> viewModel.injectJSInActiveTab(arg)
            "TYPE" -> {
                val textToType = arg.replace("\"", "\\\"").replace("'", "\\'")
                val js = """
                    (function() {
                        var inputs = document.querySelectorAll('input[type="text"], input[type="search"], textarea');
                        if (inputs.length > 0) {
                            var el = inputs[0];
                            el.focus();
                            el.value = '';
                            var i = 0;
                            var text = "$textToType";
                            var interval = setInterval(function() {
                                el.value += text.charAt(i);
                                el.dispatchEvent(new Event('input', { bubbles: true }));
                                i++;
                                if (i >= text.length) {
                                    clearInterval(interval);
                                    var form = el.closest('form');
                                    if (form) {
                                        setTimeout(function() { form.submit(); }, 500);
                                    } else {
                                        var btn = document.querySelector('button[type="submit"], input[type="submit"], button[aria-label="Search"]');
                                        if(btn) setTimeout(function() { btn.click(); }, 500);
                                    }
                                }
                            }, 100);
                        }
                    })();
                """.trimIndent()
                viewModel.injectJSInActiveTab(js)
            }
            "CLICK" -> {
                // Click the first link matching the text
                val textToClick = arg.replace("\"", "\\\"").replace("'", "\\'")
                val js = """
                    (function() {
                        var links = Array.from(document.querySelectorAll('a, button, [role="button"]'));
                        var target = links.find(el => el.innerText.toLowerCase().includes("$textToClick".toLowerCase()));
                        if (target) {
                            target.style.border = '2px solid red'; // Highlight before click
                            setTimeout(function() { target.click(); }, 500);
                        } else {
                            // If no exact match, try querySelector
                            var el = document.querySelector("$textToClick");
                            if (el) {
                                el.style.border = '2px solid red';
                                setTimeout(function() { el.click(); }, 500);
                            }
                        }
                    })();
                """.trimIndent()
                viewModel.injectJSInActiveTab(js)
            }
        }
        }
    }

    fun parseAndExecuteLLMResponse(reply: String): Pair<String, Boolean> {
        var spokenText = reply
        var hasCommand = false
            
        val searchRegex = "<SEARCH>(.*?)</SEARCH>".toRegex(RegexOption.DOT_MATCHES_ALL)
        searchRegex.find(spokenText)?.let {
            executeCommand("SEARCH", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
            
        val navigateRegex = "<NAVIGATE>(.*?)</NAVIGATE>".toRegex(RegexOption.DOT_MATCHES_ALL)
        navigateRegex.find(spokenText)?.let {
            executeCommand("NAVIGATE", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
            
        val scrollRegex = "<SCROLL>(.*?)</SCROLL>".toRegex(RegexOption.DOT_MATCHES_ALL)
        scrollRegex.find(spokenText)?.let {
            executeCommand("SCROLL", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
            
        val injectRegex = "<INJECT>(.*?)</INJECT>".toRegex(RegexOption.DOT_MATCHES_ALL)
        injectRegex.find(spokenText)?.let {
            executeCommand("INJECT", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
            
        val paymentRegex = "<PAYMENT>(.*?)</PAYMENT>".toRegex(RegexOption.DOT_MATCHES_ALL)
        paymentRegex.find(spokenText)?.let {
            spokenText = spokenText.replace(it.value, "")
        }

        val typeRegex = "<TYPE>(.*?)</TYPE>".toRegex(RegexOption.DOT_MATCHES_ALL)
        typeRegex.find(spokenText)?.let {
            executeCommand("TYPE", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }

        val clickRegex = "<CLICK>(.*?)</CLICK>".toRegex(RegexOption.DOT_MATCHES_ALL)
        clickRegex.find(spokenText)?.let {
            executeCommand("CLICK", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
        
        return Pair(spokenText.trim(), hasCommand)
    }

    fun parseAndExecuteLocalIntent(input: String): Boolean {
        val lowerInput = input.lowercase().trim()

        val openAndSearch = Regex("open (.*?) and (?:search|find|type) (?:for )?(.*)").find(lowerInput)
        if (openAndSearch != null) {
            val site = openAndSearch.groupValues[1].trim()
            val query = openAndSearch.groupValues[2].trim()
                
            when (site) {
                "amazon" -> executeCommand("NAVIGATE", "https://www.amazon.com/s?k=${URLEncoder.encode(query, "UTF-8")}")
                "youtube" -> executeCommand("NAVIGATE", "https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}")
                "google" -> executeCommand("NAVIGATE", "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")
                "chat gpt", "chatgpt" -> executeCommand("NAVIGATE", "https://chatgpt.com/?q=${URLEncoder.encode(query, "UTF-8")}")
                else -> executeCommand("SEARCH", query)
            }
            return true
        }
            
        val buyMatch = Regex("buy (?:me )?(?:a )?(.*)").find(lowerInput)
        if (buyMatch != null) {
            executeCommand("NAVIGATE", "https://www.amazon.com/s?k=" + URLEncoder.encode(buyMatch.groupValues[1], "UTF-8"))
            return true
        }

        val searchAmazonMatch = Regex("search (?:in|on) amazon (?:for )?(.*)").find(lowerInput)
        if (searchAmazonMatch != null) {
            executeCommand("NAVIGATE", "https://www.amazon.com/s?k=" + URLEncoder.encode(searchAmazonMatch.groupValues[1], "UTF-8"))
            return true
        }

        if (lowerInput.startsWith("open ") || lowerInput.startsWith("go to ")) {
            val site = lowerInput.removePrefix("open ").removePrefix("go to ").trim()
            val url = when {
                site.contains(".") -> if (site.startsWith("http")) site else "https://$site"
                site == "amazon" -> "https://www.amazon.com"
                site == "youtube" -> "https://www.youtube.com"
                site == "google" -> "https://www.google.com"
                site == "chatgpt" || site == "chat gpt" -> "https://chatgpt.com"
                else -> "https://duckduckgo.com/?q=${URLEncoder.encode(site, "UTF-8")}"
            }
            executeCommand("NAVIGATE", url)
            return true
        }
           
        val typeMatch = Regex("type (.*)").find(lowerInput)
        if (typeMatch != null) {
                val textToType = typeMatch.groupValues[1].replace("\"", "\\\"").replace("'", "\\'")
            val js = """
                (function() {
                    var inputs = document.querySelectorAll('input[type="text"], input[type="search"], textarea');
                    if (inputs.length > 0) {
                        inputs[0].value = "$textToType";
                        inputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                        var form = inputs[0].closest('form');
                        if (form) form.submit();
                        else {
                            var btn = document.querySelector('button[type="submit"], input[type="submit"], button[aria-label="Search"]');
                            if(btn) btn.click();
                        }
                    } else {
                        var editable = document.querySelector('[contenteditable="true"]');
                        if (editable) {
                            editable.textContent = "$textToType";
                            editable.dispatchEvent(new Event('input', { bubbles: true }));
                            var btn = document.querySelector('button[data-testid="send-button"], button[aria-label="Send message"]');
                            if(btn) setTimeout(function() { btn.click(); }, 500);
                        }
                    }
                })();
            """.trimIndent()
            executeCommand("INJECT", js)
            return true
        }

        return false
    }
}
