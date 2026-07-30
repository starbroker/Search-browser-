import re

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "r") as f:
    content = f.read()

old_on_done = """                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "AI_RESPONSE") {
                        if (_aiState.value == AIState.SPEAKING) {
                            _aiState.value = AIState.IDLE
                        }
                    }
                }"""

new_on_done = """                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "AI_RESPONSE") {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            if (_aiState.value == AIState.SPEAKING) {
                                _aiState.value = AIState.IDLE
                                // Auto turn on voice for live conversation
                                startListening()
                            }
                        }
                    }
                }"""

content = content.replace(old_on_done, new_on_done)

with open("app/src/main/java/com/example/ui/AIAssistant.kt", "w") as f:
    f.write(content)
