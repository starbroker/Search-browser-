with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "r") as f:
    content = f.read()

old_code = """    fun processTextPrompt(text: String) {
        val commandHandler = AICommandHandler(viewModel)
        
        // Fast path: Check for local intent"""

new_code = """    fun processTextPrompt(text: String) {
        val commandHandler = AICommandHandler(viewModel)
        viewModel.addAIChatMessage("user", text)
        
        // Fast path: Check for local intent"""

content = content.replace(old_code, new_code)

old_code2 = """                        if (hasCommand) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _aiResponseText.value = if (spokenText.isNotEmpty()) spokenText else "Executing action..."
                                if (spokenText.isNotEmpty()) {
                                    tts?.speak(spokenText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
                                }
                            }"""

new_code2 = """                        if (hasCommand) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _aiResponseText.value = if (spokenText.isNotEmpty()) spokenText else "Executing action..."
                                if (spokenText.isNotEmpty()) {
                                    viewModel.addAIChatMessage("assistant", spokenText)
                                    tts?.speak(spokenText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
                                }
                            }"""

content = content.replace(old_code2, new_code2)

old_code3 = """                        } else {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _aiState.value = AIState.SPEAKING
                                _aiResponseText.value = spokenText.ifEmpty { "Done." }
                                if (spokenText.isNotEmpty()) {
                                    val ttsResult = tts?.speak(spokenText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")"""

new_code3 = """                        } else {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _aiState.value = AIState.SPEAKING
                                _aiResponseText.value = spokenText.ifEmpty { "Done." }
                                if (spokenText.isNotEmpty()) {
                                    viewModel.addAIChatMessage("assistant", spokenText)
                                    val ttsResult = tts?.speak(spokenText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")"""

content = content.replace(old_code3, new_code3)

with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "w") as f:
    f.write(content)
