with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "r") as f:
    content = f.read()

old_code = """                        if (hasCommand) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _aiResponseText.value = "Executing action..."
                            }
                            kotlinx.coroutines.delay(4000) // Wait for action to take effect
                            isFirstTurn = false
                            continue
                        } else {"""

new_code = """                        if (hasCommand) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _aiResponseText.value = if (spokenText.isNotEmpty()) spokenText else "Executing action..."
                                if (spokenText.isNotEmpty()) {
                                    tts?.speak(spokenText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "AI_RESPONSE")
                                }
                            }
                            kotlinx.coroutines.delay(4000) // Wait for action to take effect
                            isFirstTurn = false
                            continue
                        } else {"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/AIAssistantManager.kt", "w") as f:
    f.write(content)
