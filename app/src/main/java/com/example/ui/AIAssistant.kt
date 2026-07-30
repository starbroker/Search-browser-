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
    private val apiKeys = listOf(
        com.example.BuildConfig.GROQ_API_KEY,
        com.example.BuildConfig.GROQ_API_KEY_2,
        com.example.BuildConfig.GROQ_API_KEY_3
    ).filter { it.isNotBlank() }
    
    private var currentKeyIndex = 0
    private fun getCurrentApiKey(): String = if (apiKeys.isNotEmpty()) apiKeys[currentKeyIndex] else ""
    private fun rotateKey() {
        if (apiKeys.isNotEmpty()) {
            currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private var tts: TextToSpeech? = null
    private var speechRecognizer: android.speech.SpeechRecognizer? = null
    
    private val _aiState = MutableStateFlow(AIState.IDLE)
    val aiState: StateFlow<AIState> = _aiState
    
    private val _aiResponseText = MutableStateFlow("")
    val aiResponseText: StateFlow<String> = _aiResponseText

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentChatJob: Job? = null

    init {
        tts = TextToSpeech(context, this)
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _aiState.value = AIState.THINKING
                    }
                    override fun onError(error: Int) {
                        Log.e("AIAssistant", "Speech recognition error: $error")
                        if (error == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
                            _aiState.value = AIState.IDLE
                            _aiResponseText.value = ""
                        } else {
                            _aiState.value = AIState.ERROR
                            _aiResponseText.value = "Error listening"
                        }
                    }
                    override fun onResults(results: android.os.Bundle?) {
                        val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            _aiResponseText.value = "You: $text"
                            processTextPrompt(text, isVoice = true)
                        } else {
                            _aiState.value = AIState.IDLE
                        }
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {
                        val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _aiResponseText.value = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                })
            }
        }
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
                    tts?.setPitch(1.0f)
                }
            } else {
                tts?.setPitch(1.0f)
            }
            tts?.setSpeechRate(0.85f)
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "AI_RESPONSE") {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (_aiState.value == AIState.SPEAKING) {
                                if (autoListenAfterSpeak) {
                                    startListening()
                                } else {
                                    _aiState.value = AIState.IDLE
                                }
                            }
                        }
                    }
                }
                override fun onError(utteranceId: String?) {
                    _aiState.value = AIState.ERROR
                }
            })
        }
    }

    private var autoListenAfterSpeak = false

    fun startListening() {
        if (_aiState.value == AIState.LISTENING) return
        
        autoListenAfterSpeak = true
        // Interrupt previous TTS/tasks
        stopSpeaking()
        currentChatJob?.cancel()

        _aiState.value = AIState.LISTENING
        _aiResponseText.value = "Listening..."
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        }
    }

    fun stopListeningAndProcess() {
        autoListenAfterSpeak = false
        if (_aiState.value != AIState.LISTENING) return
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            speechRecognizer?.stopListening()
        }
    }

    // processAudioFile removed

    fun processTextPrompt(text: String, isVoice: Boolean = false) {
        autoListenAfterSpeak = isVoice
        val commandHandler = AICommandHandler(viewModel)
        viewModel.addAIChatMessage("user", text)
        
        // Fast path: Check for local intent
        if (commandHandler.parseAndExecuteLocalIntent(text)) {
            _aiResponseText.value = "Done."
            _aiState.value = AIState.SPEAKING
            viewModel.addAIChatMessage("assistant", "Opening now.")
            val ttsResult = tts?.speak("Opening now.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, android.os.Bundle(), "AI_RESPONSE")
            if (ttsResult == null || ttsResult == android.speech.tts.TextToSpeech.ERROR) {
                if (autoListenAfterSpeak) {
                    startListening()
                } else {
                    _aiState.value = AIState.IDLE
                }
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
                    You are Storm AI, an advanced autonomous AI assistant built into the Storm Web Browser. You must act and talk like the absolute best AI assistants (e.g., Google Assistant, Gemini).
                    You are conversational, helpful, real-time, and you speak your answers aloud.
                    
                    CRITICAL RULES:
                    1. TALK LIKE AN ASSISTANT: Always respond with friendly, natural, conversational text. The text you output without command tags will be READ ALOUD to the user.
                    2. DO NOT USE MARKDOWN in your spoken text. (No **bolding** or *italics* because it will be spoken by a text-to-speech engine).
                    3. COMPLETE TASKS: You must execute multiple steps if necessary to finish a task. You will be called repeatedly until you provide an output with NO commands.
                    4. TYPE IN INPUT BARS: Use <TYPE>text</TYPE> to type into search bars, input fields, or text editors.
                    5. READ AND CLICK LINKS: Read the CURRENT WEBPAGE TEXT carefully. If the user wants you to open something, find the exact link text and output <CLICK>Exact Link Text</CLICK>.
                    6. ADD TEXT TO DOCUMENTS: You can use <INJECT>javascript</INJECT> to manipulate the DOM, such as adding bold text to a document if requested.
                    7. ASK THE USER: If you need clarification to complete a task, ask the user a question in your spoken text.
                    8. REAL-TIME CONVERSATION: If the user just wants information (like "what is the weather" or "summarize this page"), read the CURRENT WEBPAGE TEXT, summarize it concisely, and speak it aloud naturally WITHOUT commands.
                    9. EXPRESSIVE SPEECH: Use expressive words, conversational fillers, and punctuation (like ... or !) to make your spoken text sound highly emotive and natural.
                    10. CLOSE POPUPS: If the user asks you to close a popup or ad, you can use <INJECT>document.querySelectorAll('.close, .dismiss, [aria-label=\"Close\"], [class*=\"close\"], [id*=\"close\"], button').forEach(el => { if(el.innerText && el.innerText.toLowerCase().includes('close')) el.click(); else el.click(); });</INJECT>.
                    11. COOKIES CONSENT: If the user asks you to manage cookies, use <INJECT>document.querySelectorAll('button, a').forEach(b => { if(b.innerText && (b.innerText.toLowerCase().includes('manage') || b.innerText.toLowerCase().includes('options') || b.innerText.toLowerCase().includes('necessary'))) b.click() })</INJECT>. If there is a popup, try to close it.
                    12. SECURE NAVIGATION: Only navigate to secure HTTPS pages. Do NOT go to unsecure HTTP pages.
                    13. SCROLL WHILE READING: If you are reading text or talking about a specific part of a long page, use the <SCROLL>down</SCROLL> command alongside your spoken text so the user can see what you are reading. You can scroll down to read more, find products, or see links.
                    14. SEARCH FALLBACK: If the default SEARCH command is not working or not accurate, switch to <NAVIGATE>https://www.google.com/search?q=your+query</NAVIGATE>.
                    15. PRECISE FINDING: Do not stop trying until you accurately find what the user wants. Use multiple steps if needed.
                    16. SWITCHING CATEGORIES: If you need to switch to Images, Videos, News, Shopping, etc., use <CLICK>Images</CLICK> or the respective tab name.
                    17. READING BLOGS: You can read blogs and news by clicking on the links and summarizing the content.
                    18. SCROLLING FOR LINKS: If you cannot find a link or information on the current screen, use <SCROLL>down</SCROLL> to load more links or read more content before giving up.
                    19. SHOPPING & E-COMMERCE: You can click "Add to Cart", "Buy Now", or select options by using the <CLICK> command (e.g., <CLICK>Add to Cart</CLICK>).
                    20. SEARCHING: Use <SEARCH>query</SEARCH> to search the web. You can fully customize the text input for search based on what the user wants.
                    21. TEXT INPUT & INTERACTION: You can use <TYPE>custom text</TYPE> to input text into search bars, input fields, and forms. You can freely interact with other websites using <CLICK>, <TYPE>, <NAVIGATE>, and <SCROLL> to do whatever the user needs you to do.
                    22. SENSITIVE INFO: Do not try to type or submit login credentials or payment details. If requested, stop and politely ask the user to enter those details themselves.
                    23. BUYING PROCESS: If the user asks you to buy an item (e.g., 'buy me a cola'), you must first search for it (e.g., <SEARCH>cola</SEARCH> or <NAVIGATE>https://www.amazon.com/s?k=cola</NAVIGATE>), then read the page to find a suitable product link and <CLICK> it, and finally click the 'Buy Now' or 'Add to Cart' button.

                    Available Commands (use exact syntax):
                    <NAVIGATE>https://example.com</NAVIGATE> : Opens a URL.
                    <ADD_TAB>https://example.com</ADD_TAB> : Opens a new tab with the URL.
                    <TYPE>text</TYPE> : Types text into the active input bar.
                    <CLICK>Link Text</CLICK> : Clicks a link or button on the screen matching the text.
                    <SCROLL>down</SCROLL> or <SCROLL>up</SCROLL> : Scrolls the page.
                    <INJECT>javascript code</INJECT> : Runs JavaScript on the page (e.g., to add bold text or modify DOM).
                    <SEARCH>query</SEARCH> : Performs a search in the default search engine.
                    <OPEN>panel_name</OPEN> : Opens internal browser menus like "shield", "menu", "settings", "history", "bookmarks", "downloads".

                    Remember: Text outside of commands is SPOKEN. Commands are EXECUTED. Combine them effectively!
                """.trimIndent()
                
                messagesArray.put(org.json.JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                
                val history = viewModel.getAIChatHistory()
                for (msg in history.takeLast(10)) {
                    messagesArray.put(org.json.JSONObject().apply {
                        put("role", msg["role"])
                        put("content", msg["content"])
                    })
                }

                while (iterations < maxIterations) {
                    iterations++

                    val rawContent = viewModel.getActiveTabContent()
                    val pageContent = if (rawContent.length > 8000) rawContent.substring(0, 8000) + "...(truncated)" else rawContent
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
                        put("max_tokens", 2048)
                        put("stream", true)
                    }

                    val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull())
                    var response: Response? = null
                    var attempt = 0
                    while (attempt < apiKeys.size) {
                        val request = okhttp3.Request.Builder()
                            .url("https://api.groq.com/openai/v1/chat/completions")
                            .addHeader("Authorization", "Bearer ${getCurrentApiKey()}")
                            .post(requestBody)
                            .build()
                        response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            break
                        } else {
                            val errorBody = response.body?.string() ?: "Unknown error"
                            android.util.Log.e("AIAssistant", "API Error: $errorBody")
                            response.close()
                            if (attempt == apiKeys.size - 1) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    _aiResponseText.value = "I'm having trouble connecting right now. Please try again."
                                    _aiState.value = AIState.IDLE
                                }
                                return@launch
                            }
                            rotateKey()
                            attempt++
                        }
                    }
                    
                    if (response?.isSuccessful == true) {
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(response.body?.byteStream()))
                        val fullReply = java.lang.StringBuilder()
                        var line = reader.readLine()
                        while (line != null) {
                            if (line.startsWith("data: ")) {
                                if (line == "data: [DONE]") {
                                    break
                                }
                                try {
                                    val json = org.json.JSONObject(line.substring(6))
                                    val content = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content", "") ?: ""
                                    fullReply.append(content)
                                    
                                    var intermediateText = fullReply.toString()
                                    intermediateText = intermediateText.replace(Regex("(?i)<SEARCH>.*?</SEARCH>", RegexOption.DOT_MATCHES_ALL), "")
                                    intermediateText = intermediateText.replace(Regex("(?i)<NAVIGATE>.*?</NAVIGATE>", RegexOption.DOT_MATCHES_ALL), "")
                                    intermediateText = intermediateText.replace(Regex("(?i)<ADD_TAB>.*?</ADD_TAB>", RegexOption.DOT_MATCHES_ALL), "")
                                    intermediateText = intermediateText.replace(Regex("(?i)<SCROLL>.*?</SCROLL>", RegexOption.DOT_MATCHES_ALL), "")
                                    intermediateText = intermediateText.replace(Regex("(?i)<INJECT>.*?</INJECT>", RegexOption.DOT_MATCHES_ALL), "")
                                    intermediateText = intermediateText.replace(Regex("(?i)<TYPE>.*?</TYPE>", RegexOption.DOT_MATCHES_ALL), "")
                                    intermediateText = intermediateText.replace(Regex("(?i)<CLICK>.*?</CLICK>", RegexOption.DOT_MATCHES_ALL), "")
                                    intermediateText = intermediateText.replace(Regex("(?i)<OPEN>.*?</OPEN>", RegexOption.DOT_MATCHES_ALL), "")
                                    if (intermediateText.isNotBlank()) {
                                        _aiResponseText.value = intermediateText.trim()
                                    } else {
                                        _aiResponseText.value = "Thinking..."
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
                                    tts?.speak(spokenText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, android.os.Bundle(), "AI_RESPONSE")
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
                                    val ttsResult = tts?.speak(spokenText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, android.os.Bundle(), "AI_RESPONSE")
                                    if (ttsResult == null || ttsResult == android.speech.tts.TextToSpeech.ERROR) {
                                        if (autoListenAfterSpeak) {
                                            startListening()
                                        } else {
                                            _aiState.value = AIState.IDLE
                                        }
                                    }
                                } else {
                                    if (autoListenAfterSpeak) {
                                        startListening()
                                    } else {
                                        _aiState.value = AIState.IDLE
                                    }
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
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (_aiState.value == AIState.THINKING) {
                        _aiState.value = AIState.IDLE
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
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                speechRecognizer?.stopListening()
            }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        _aiState.value = AIState.IDLE
    }
}




class AICommandHandler(private val viewModel: BrowserViewModel) {

    fun executeCommand(commandType: String, arg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
        when (commandType.uppercase()) {
            "SEARCH" -> viewModel.performSearchFromAI(arg)
            "NAVIGATE" -> viewModel.navigateActiveTabFromAI(arg)
            "ADD_TAB" -> viewModel.addNewTab(arg)
            "OPEN_SHIELD" -> viewModel.openShield()
            "OPEN_MENU" -> viewModel.openMenuDrawer()
            "OPEN_SETTINGS" -> viewModel.openSettings()
            "OPEN_HISTORY" -> viewModel.openHistory()
            "OPEN_BOOKMARKS" -> viewModel.openBookmarks()
            "OPEN_DOWNLOADS" -> viewModel.openDownloads()
            "SCROLL" -> {
                val js = if (arg.contains("down", ignoreCase = true)) "window.scrollBy({top: window.innerHeight * 0.8, behavior: 'smooth'});" else "window.scrollBy({top: -window.innerHeight * 0.8, behavior: 'smooth'});"
                viewModel.injectJSInActiveTab(js)
            }
            "INJECT" -> viewModel.injectJSInActiveTab(arg)
            "TYPE" -> {
                val textToType = arg.replace("\"", "\\\"").replace("'", "\\'")
                val js = """
                    (function() {
                        var inputs = document.querySelectorAll('input[type="text"], input[type="search"], input[type="email"], input[type="password"], textarea, [contenteditable="true"]');
                        if (inputs.length > 0) {
                            var el = inputs[0];
                            for (var j = 0; j < inputs.length; j++) {
                                if (inputs[j].offsetParent !== null && !inputs[j].disabled) {
                                    el = inputs[j];
                                    break;
                                }
                            }
                            el.focus();
                            el.value = '';
                            var i = 0;
                            var text = "$textToType";
                            var interval = setInterval(function() {
                                el.value += text.charAt(i);
                                el.dispatchEvent(new Event('input', { bubbles: true }));
                                el.dispatchEvent(new Event('change', { bubbles: true }));
                                i++;
                                if (i >= text.length) {
                                    clearInterval(interval);
                                    var form = el.closest('form');
                                    if (form) {
                                        setTimeout(function() { form.submit(); }, 500);
                                    } else {
                                        var btn = document.querySelector('button[type="submit"], input[type="submit"], button[aria-label*="Search" i], button[aria-label*="Submit" i]');
                                        if(btn) setTimeout(function() { btn.click(); }, 500);
                                        else {
                                           el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                                        }
                                    }
                                }
                            }, 100);
                        }
                    })();
                """.trimIndent()
                viewModel.injectJSInActiveTab(js)
            }
            "CLICK" -> {
                // Click the first link matching the text and precisely scroll to it
                val textToClick = arg.replace("\"", "\\\"").replace("'", "\\'")
                val js = """
                    (function() {
                        var links = Array.from(document.querySelectorAll('a, button, [role="button"], input[type="button"], input[type="submit"]'));
                        var target = links.find(el => 
                            (el.innerText && el.innerText.toLowerCase().includes("$textToClick".toLowerCase())) ||
                            (el.getAttribute('aria-label') && el.getAttribute('aria-label').toLowerCase().includes("$textToClick".toLowerCase())) ||
                            (el.getAttribute('title') && el.getAttribute('title').toLowerCase().includes("$textToClick".toLowerCase())) ||
                            (el.getAttribute('value') && el.getAttribute('value').toLowerCase().includes("$textToClick".toLowerCase()))
                        );
                        if (!target) {
                            var allEls = Array.from(document.querySelectorAll('span, div, img'));
                            target = allEls.find(el => 
                                (el.innerText && el.innerText.toLowerCase().includes("$textToClick".toLowerCase()) && el.innerText.length < 50) ||
                                (el.getAttribute('alt') && el.getAttribute('alt').toLowerCase().includes("$textToClick".toLowerCase()))
                            );
                        }
                        
                        if (target) {
                            target.scrollIntoView({behavior: "smooth", block: "center", inline: "nearest"});
                            target.style.outline = '2px solid red'; // Highlight before click
                            setTimeout(function() { target.click(); }, 1500); // give time to see scroll
                        } else {
                            // If no exact match, try querySelector
                            try {
                                var el = document.querySelector("$textToClick");
                                if (el) {
                                    el.scrollIntoView({behavior: "smooth", block: "center", inline: "nearest"});
                                    el.style.outline = '2px solid red';
                                    setTimeout(function() { el.click(); }, 1500);
                                }
                            } catch(e) {}
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
            
        val searchRegex = "(?i)<SEARCH>(.*?)</SEARCH>".toRegex(RegexOption.DOT_MATCHES_ALL)
        searchRegex.findAll(spokenText).forEach {
            executeCommand("SEARCH", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
            
        val navigateRegex = "(?i)<NAVIGATE>(.*?)</NAVIGATE>".toRegex(RegexOption.DOT_MATCHES_ALL)
        var navCount = 0
        navigateRegex.findAll(spokenText).forEach {
            if (navCount == 0) {
                executeCommand("NAVIGATE", it.groupValues[1])
            } else {
                executeCommand("ADD_TAB", it.groupValues[1])
            }
            navCount++
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
            
        val addTabRegex = "(?i)<ADD_TAB>(.*?)</ADD_TAB>".toRegex(RegexOption.DOT_MATCHES_ALL)
        addTabRegex.findAll(spokenText).forEach {
            executeCommand("ADD_TAB", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }

        val scrollRegex = "(?i)<SCROLL>(.*?)</SCROLL>".toRegex(RegexOption.DOT_MATCHES_ALL)
        scrollRegex.findAll(spokenText).forEach {
            executeCommand("SCROLL", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
            
        val injectRegex = "(?i)<INJECT>(.*?)</INJECT>".toRegex(RegexOption.DOT_MATCHES_ALL)
        injectRegex.findAll(spokenText).forEach {
            executeCommand("INJECT", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
            
        val paymentRegex = "(?i)<PAYMENT>(.*?)</PAYMENT>".toRegex(RegexOption.DOT_MATCHES_ALL)
        paymentRegex.findAll(spokenText).forEach {
            spokenText = spokenText.replace(it.value, "")
        }

        val typeRegex = "(?i)<TYPE>(.*?)</TYPE>".toRegex(RegexOption.DOT_MATCHES_ALL)
        typeRegex.findAll(spokenText).forEach {
            executeCommand("TYPE", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }

        val clickRegex = "(?i)<CLICK>(.*?)</CLICK>".toRegex(RegexOption.DOT_MATCHES_ALL)
        clickRegex.findAll(spokenText).forEach {
            executeCommand("CLICK", it.groupValues[1])
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
        
        val openRegex = "(?i)<OPEN>(.*?)</OPEN>".toRegex(RegexOption.DOT_MATCHES_ALL)
        openRegex.findAll(spokenText).forEach {
            val panel = it.groupValues[1].lowercase()
            when {
                panel.contains("shield") -> executeCommand("OPEN_SHIELD", "")
                panel.contains("menu") -> executeCommand("OPEN_MENU", "")
                panel.contains("settings") -> executeCommand("OPEN_SETTINGS", "")
                panel.contains("history") -> executeCommand("OPEN_HISTORY", "")
                panel.contains("bookmark") -> executeCommand("OPEN_BOOKMARKS", "")
                panel.contains("download") -> executeCommand("OPEN_DOWNLOADS", "")
            }
            spokenText = spokenText.replace(it.value, "")
            hasCommand = true
        }
        
        return Pair(spokenText.trim(), hasCommand)
    }

    fun parseAndExecuteLocalIntent(input: String): Boolean {
        val lowerInput = input.lowercase().trim()
        if (lowerInput.contains("open shield") || lowerInput.contains("show shield") || lowerInput.contains("shield menu")) {
            executeCommand("OPEN_SHIELD", "")
            return true
        }
        if (lowerInput.contains("open menu") || lowerInput.contains("show menu") || lowerInput.contains("menu drawer")) {
            executeCommand("OPEN_MENU", "")
            return true
        }
        if (lowerInput.contains("open settings") || lowerInput.contains("show settings")) {
            executeCommand("OPEN_SETTINGS", "")
            return true
        }
        if (lowerInput.contains("open history") || lowerInput.contains("show history")) {
            executeCommand("OPEN_HISTORY", "")
            return true
        }
        if (lowerInput.contains("open bookmarks") || lowerInput.contains("show bookmarks")) {
            executeCommand("OPEN_BOOKMARKS", "")
            return true
        }
        if (lowerInput.contains("open downloads") || lowerInput.contains("show downloads")) {
            executeCommand("OPEN_DOWNLOADS", "")
            return true
        }
        return false
    }
}
