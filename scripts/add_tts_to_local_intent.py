with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "r") as f:
    content = f.read()

old_code = """        // Fast path: Check for local intent
        if (commandHandler.parseAndExecuteLocalIntent(text)) {
            _aiState.value = AIState.IDLE
            _aiResponseText.value = "Done."
            return
        }"""

new_code = """        // Fast path: Check for local intent
        if (commandHandler.parseAndExecuteLocalIntent(text)) {
            _aiResponseText.value = "Done."
            _aiState.value = AIState.SPEAKING
            val ttsResult = tts?.speak("Opening now.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
            if (ttsResult == android.speech.tts.TextToSpeech.ERROR) {
                _aiState.value = AIState.IDLE
            }
            return
        }"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "w") as f:
    f.write(content)
